/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ingenieux.mojo.lambda;

import br.com.ingenieux.mojo.aws.util.GlobUtil;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.s3.AmazonS3URI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.codehaus.plexus.util.StringUtils.isBlank;

/**
 * <p>Represents the AWS Lambda Deployment Process, which means:</p>
 * <p/>
 * <ul>
 * <li>Parsing the function-definition file</li>
 * <li>For each declared function, tries to update the function</li>
 * <li>if the function is missing, create it</li>
 * <li>Otherwise, compare the function definition with the expected parameters, and changes the function configuration if needed</li>
 * </ul>
 */
@Mojo(name = "deploy-functions")
public class DeployMojo extends AbstractLambdaMojo {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Lambda Function URL on S3, e.g. <code>s3://somebucket/object/key/path.zip</code>
     */
    @Parameter(required = true, property = "lambda.s3url")
    String s3Url;

    /**
     * AWS Lambda Default Timeout, in seconds (used when missing in function definition)
     */
    @Parameter(required = true, property = "lambda.default.timeout", defaultValue = "5")
    Integer defaultTimeout;

    /**
     * AWS Lambda Default Memory Size, in MB (used when missing in function definition)
     */
    @Parameter(required = true, property = "lambda.default.memorySize", defaultValue = "128")
    Integer defaultMemorySize;

    /**
     * <p>AWS Lambda Default IAM Role (used when missing in function definition)</p>
     *
     * <p>Allows wildcards like '*' and '?' - will be looked up upon when deploying</p>
     */
    @Parameter(required = true, property = "lambda.default.role", defaultValue = "arn:aws:iam::*:role/lambda_basic_execution")
    String defaultRole;

    /**
     * <p>Definition File</p>
     * <p/>
     * <p>Consists of a JSON file array as such:</p>
     * <p/>
     * <pre>[ {
     *   "name": "AWS Function Name",
     *   "handler": "AWS Function Handler ref",
     *   "timeout": 5,
     *   "memorySize": 128,
     *   "role": "aws role"
     * }
     * ]</pre>
     * <p/>
     * <p>Where:</p>
     * <p/>
     * <ul>
     * <li>Name is the AWS Lambda Function Name</li>
     * <li>Handler is the Handler Ref (for Java, it is <code>classname::functionName</code>)</li>
     * <li>Timeout is the timeout</li>
     * <li>memorySize is the memory </li>
     * <li>Role is the AWS Service Role</li>
     * </ul>
     * <p/>
     * <p>Of those, only <code>name</code> and <code>handler</code> are obligatory.</p>
     */
    @Parameter(required = true, property = "lambda.definition.file", defaultValue = "${project.build.outputDirectory}/META-INF/lambda-definitions.json")
    File definitionFile;

    private AWSLambdaClient lambdaClient;

    private AmazonIdentityManagementClient iamClient;

    private Set<String> roles;

    private AmazonS3URI s3Uri;

//    /**
//     * Glob of Functions to Include (default: all)
//     */
//    @Parameter(property="lambda.function.includes")
//    List<String> includes = Collections.singletonList("*");
//
//    /**
//     * Glob of Functions to Exclude (default: empty)
//     */
//    @Parameter(property="lambda.function.excludes")
//    List<String> excludes = Collections.emptyList();

    @Override
    protected void configure() {
        super.configure();

        try {
            configureInternal();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private void configureInternal() throws MojoExecutionException {
        lambdaClient = this.getService();
        iamClient = createServiceFor(AmazonIdentityManagementClient.class);
        s3Uri = new AmazonS3URI(s3Url);

        roles = loadRoles();

        defaultRole = lookupRoleGlob(defaultRole);
    }

    private Set<String> loadRoles() {
        Set<String> result = new TreeSet<String>();

        boolean done = false;
        String marker = null;
        do {
            final ListRolesRequest listRolesRequest = new ListRolesRequest();

            listRolesRequest.setMarker(marker);

            final ListRolesResult listRolesResult = iamClient.listRoles(listRolesRequest);

            for (Role r : listRolesResult.getRoles()) {
                result.add(r.getArn());
            }

            done = (!listRolesResult.isTruncated());

            marker = listRolesResult.getMarker();
        } while (!done);

        return result;
    }

    @Override
    protected Object executeInternal() throws Exception {
        Map<String, LambdaFunctionDefinition> functionDefinitions = parseFunctionDefinions();

        String s3Bucket = s3Uri.getBucket();
        String s3Key = s3Uri.getKey();

        for (LambdaFunctionDefinition d : functionDefinitions.values()) {
            getLog().info(format("Deploying Function: %s (handler: %s)", d.getName(), d.getHandler()));

            try {
                final UpdateFunctionCodeResult updateFunctionCodeResult = lambdaClient.updateFunctionCode(new UpdateFunctionCodeRequest().withFunctionName(d.getName()).withS3Bucket(s3Bucket).withS3Key(s3Key));

                updateIfNeeded(d, updateFunctionCodeResult);
            } catch (ResourceNotFoundException exc) {
                getLog().info("Function does not exist. Creating it instead.");

                createFunction(d);
            }
        }

        return functionDefinitions;
    }

    private CreateFunctionResult createFunction(LambdaFunctionDefinition d) {
        CreateFunctionRequest req = new CreateFunctionRequest().
                withCode(new FunctionCode().withS3Bucket(s3Uri.getBucket()).withS3Key(s3Uri.getKey())).
                withDescription(d.getDescription()).
                withFunctionName(d.getName()).
                withHandler(d.getHandler()).
                withMemorySize(d.getMemorySize()).
                withRole(d.getRole()).
                withRuntime(Runtime.Java8).
                withTimeout(d.getTimeout());

        final CreateFunctionResult createFunctionResult = lambdaClient.createFunction(req);

        return createFunctionResult;
    }

    private UpdateFunctionConfigurationResult updateIfNeeded(LambdaFunctionDefinition d, UpdateFunctionCodeResult curFc) {
        boolean bEquals = new EqualsBuilder().
                append(d.getDescription(), curFc.getDescription()).
                append(d.getHandler(), curFc.getHandler()).
                append(d.getMemorySize(), curFc.getMemorySize().intValue()).
                append(d.getRole(), curFc.getRole()).
                append(d.getTimeout(), curFc.getTimeout().intValue()).isEquals();

        if (!bEquals) {
            final UpdateFunctionConfigurationRequest updRequest = new UpdateFunctionConfigurationRequest();

            updRequest.setFunctionName(d.getName());
            updRequest.setDescription(d.getDescription());
            updRequest.setHandler(d.getHandler());
            updRequest.setMemorySize(d.getMemorySize());
            updRequest.setRole(d.getRole());
            updRequest.setTimeout(d.getTimeout());

            getLog().info(format("Function Configuration doesn't match expected defaults. Updating it to %s.", updRequest));

            final UpdateFunctionConfigurationResult result = lambdaClient.updateFunctionConfiguration(updRequest);

            return result;
        }

        return null;
    }

    private Map<String, LambdaFunctionDefinition> parseFunctionDefinions() throws Exception {
        String source = IOUtils.toString(new FileInputStream(definitionFile));

        source = new StrSubstitutor(this.getPluginContext()).replace(source);

        getLog().info(format("Loaded and replaced definitions from file '%s'", definitionFile.getPath()));

        List<LambdaFunctionDefinition> definitionList = OBJECT_MAPPER.readValue(source, new TypeReference<List<LambdaFunctionDefinition>>() {});

        getLog().info(format("Found %d definitions: ", definitionList.size()));

        Map<String, LambdaFunctionDefinition> result = new TreeMap<String, LambdaFunctionDefinition>();

        for (LambdaFunctionDefinition d : definitionList) {
            if (0 == d.getMemorySize()) {
                d.setMemorySize(defaultMemorySize);
            }

            if (isBlank(d.getRole())) {
                d.setRole(defaultRole);
            } else {
                d.setRole(lookupRoleGlob(d.getRole()));
            }

            if (0 == d.getTimeout()) {
                d.setTimeout(defaultTimeout);
            }

            result.put(d.getName(), d);
        }

        getLog().info(format("Merged into %d definitions: ", result.size()));

        return result;
    }

    private String lookupRoleGlob(String role) {
        if (GlobUtil.hasWildcards(role)) {
            getLog().info(format("Looking up IAM Role '%s'", role));

            Pattern p = GlobUtil.globify(role);

            for (String s : roles) {
                if (p.matcher(s).matches()) {
                    getLog().info(format("Found Role: '%s'", s));

                    return s;
                }
            }

            throw new IllegalStateException("Unable to lookup role '" + role + "': Not found");
        } else {
            getLog().info(format("Using Role as is: '%s'", role));

            return role;
        }
    }
}

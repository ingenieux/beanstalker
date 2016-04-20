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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.ResourceConflictException;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.com.ingenieux.mojo.aws.util.RoleResolver;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.codehaus.plexus.util.StringUtils.isBlank;

/**
 * <p>Represents the AWS Lambda Deployment Process, which means:</p> <p/> <ul> <li>Parsing the
 * function-definition file</li> <li>For each declared function, tries to update the function</li>
 * <li>if the function is missing, create it</li> <li>Otherwise, compare the function definition
 * with the expected parameters, and changes the function configuration if needed</li> </ul>
 */
@Mojo(name = "deploy-functions")
public class DeployMojo extends AbstractLambdaMojo {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Lambda Function URL on S3, e.g. <code>s3://somebucket/object/key/path.zip</code>
     */
    @Parameter(required = true, property = "lambda.s3url", defaultValue = "${beanstalk.lastUploadedS3Object}")
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
     * Publish a new function version?
     */
    @Parameter(property = "lambda.deploy.publish", defaultValue = "false")
    Boolean deployPublish;

    /**
     * Publish a new function version?
     */
    @Parameter(property = "lambda.deploy.aliases", defaultValue = "false")
    Boolean deployAliases;

    /**
     * <p>Definition File</p> <p/> <p>Consists of a JSON file array as such:</p> <p/>
     * <pre>[ {
     *   "name": "AWS Function Name",
     *   "handler": "AWS Function Handler ref",
     *   "timeout": 5,
     *   "memorySize": 128,
     *   "role": "aws role"
     * }
     * ]</pre>
     * <p/> <p>Where:</p> <p/> <ul> <li>Name is the AWS Lambda Function Name</li> <li>Handler is the
     * Handler Ref (for Java, it is <code>classname::functionName</code>)</li> <li>Timeout is the
     * timeout</li> <li>memorySize is the memory </li> <li>Role is the AWS Service Role</li> </ul>
     * <p/> <p>Of those, only <code>name</code> and <code>handler</code> are obligatory.</p>
     */
    @Parameter(required = true, property = "lambda.definition.file", defaultValue = "${project.build.outputDirectory}/META-INF/lambda-definitions.json")
    File definitionFile;

    /**
     * Security Group Ids
     */
    @Parameter(property = "lambda.deploy.securityGroupIds", defaultValue = "")
    List<String> securityGroupIds = new ArrayList<>();

    public void setSecurityGroupIds(String securityGroupIds) {
        List<String> securityGroupIdsAsList = asList(securityGroupIds.split(","));

        this.securityGroupIds.addAll(securityGroupIdsAsList);
    }

    /**
     * Subnet Ids
     */
    @Parameter(property = "lambda.deploy.subnetIds", defaultValue = "")
    List<String> subnetIds;

    public void setSubnetIds(String subnetIds) {
        List<String> subnetIdsAsList = asList(subnetIds.split(","));

        this.subnetIds.addAll(subnetIdsAsList);
    }

    private AWSLambdaClient lambdaClient;

    private AmazonS3URI s3Uri;

    private RoleResolver roleResolver;

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

        roleResolver = new RoleResolver(createServiceFor(AmazonIdentityManagementClient.class));

        s3Uri = new AmazonS3URI(s3Url);

        defaultRole = roleResolver.lookupRoleGlob(defaultRole);
    }

    @Override
    protected Object executeInternal() throws Exception {
        Map<String, LambdaFunctionDefinition> functionDefinitions = parseFunctionDefinions();

        String s3Bucket = s3Uri.getBucket();
        String s3Key = s3Uri.getKey();

        for (LambdaFunctionDefinition d : functionDefinitions.values()) {
            getLog().info(format("Deploying Function: %s (handler: %s)", d.getName(), d.getHandler()));

            String version = null;

            try {
                final UpdateFunctionCodeRequest
                        updateFunctionCodeRequest =
                        new UpdateFunctionCodeRequest().
                                withFunctionName(d.getName()).
                                withS3Bucket(s3Bucket).
                                withPublish(this.deployPublish).
                                withS3Key(s3Key);
                final UpdateFunctionCodeResult updateFunctionCodeResult = lambdaClient.updateFunctionCode(
                        updateFunctionCodeRequest);

                d.setArn(updateFunctionCodeResult.getFunctionArn());

                d.setVersion(version = updateFunctionCodeResult.getVersion());

                updateIfNeeded(d, updateFunctionCodeResult);
            } catch (ResourceNotFoundException exc) {
                getLog().info("Function does not exist. Creating it instead.");

                final CreateFunctionResult function = createFunction(d);

                d.setArn(function.getFunctionArn());

                d.setVersion(version = function.getVersion());
            }

            if (isNotBlank(d.getAlias()) && (deployAliases)) {
                updateAlias(d.getName(), version, d.getAlias());
            }

            try {
                if (null != d.getBindings() && !d.getBindings().isEmpty()) {
                    deployBindings(d);
                }
            } catch (Exception exc) {
                getLog().warn("Failure. Skipping. ", exc);
            }
        }

        return functionDefinitions;
    }

    private void deployBindings(LambdaFunctionDefinition d) throws Exception {
        for (String binding : d.getBindings()) {
            Arn arn = Arn.lookupArn(binding);

            if (isNotBlank(d.getAlias())) {
                arn = Arn.lookupArn(d.getAlias());
            }

            if (null == arn) {
                getLog().warn("Unable to find binding for arn: " + arn);

                continue;
            }

            switch (arn.getService()) {
                case "sns": {
                    updateSNSFunction(arn, d);
                    break;
                }
                case "dynamodb": {
                    updateDynamoDBFunction(arn, d);
                    break;
                }
                case "kinesis": {
                    updateKinesisFunction(arn, d);
                    break;
                }
                case "cognito": {
                    updateCognitoFunction(arn, d);
                    break;
                }
                case "s3": {
                    updateS3Function(arn, d);
                }
            }
        }
    }

    private void updateS3Function(Arn bindingArn, LambdaFunctionDefinition d) throws Exception {
        throw new NotImplementedException("We don't support S3 yet. Sorry. :/");
    }

    private void updateCognitoFunction(Arn bindingArn, LambdaFunctionDefinition d) throws Exception {
        throw new NotImplementedException("We don't support Cognito yet. Sorry. :/");
    }

    private void updateKinesisFunction(Arn bindingArn, LambdaFunctionDefinition d) throws Exception {
        throw new NotImplementedException("AWS SDK for Java doesn't support Kinesis Streams yet. Sorry. :/");
    }

    private void updateDynamoDBFunction(Arn bindingArn, LambdaFunctionDefinition d) throws Exception {
        throw new NotImplementedException("AWS SDK for Java doesn't support DynamoDB Streams yet. Sorry. :/");
    }

    private void updateSNSFunction(Arn bindingArn, LambdaFunctionDefinition d) throws Exception {
        AmazonSNSClient client = createServiceFor(AmazonSNSClient.class);

        client.setRegion(Region.getRegion(Regions.fromName(bindingArn.getRegion())));

        SubscribeRequest req = new SubscribeRequest()
                .withTopicArn(bindingArn.getSourceArn())
                .withProtocol("lambda")
                .withEndpoint(d.getArn());

        final SubscribeResult subscribe = client.subscribe(req);

        getLog().info("Subscribed topic arn " + bindingArn.getSourceArn() + " to function " + d.getArn());

        // TODO: Unsubscribe older versions

    }

    protected Object updateAlias(String functionName, String version, String alias) {
        try {
            CreateAliasRequest req = new CreateAliasRequest().
                    withFunctionName(functionName).
                    withFunctionVersion(version).
                    withName(alias);

            return lambdaClient.createAlias(req);
        } catch (ResourceConflictException exc) {
            UpdateAliasRequest req = new UpdateAliasRequest().
                    withFunctionName(functionName).
                    withFunctionVersion(version).
                    withName(alias);

            return lambdaClient.updateAlias(req);
        }
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
                withPublish(this.deployPublish).
                withVpcConfig(new VpcConfig().withSecurityGroupIds(securityGroupIds).withSubnetIds(subnetIds)).
                withTimeout(d.getTimeout());

        final CreateFunctionResult createFunctionResult = lambdaClient.createFunction(req);

        return createFunctionResult;
    }

    private UpdateFunctionConfigurationResult updateIfNeeded(LambdaFunctionDefinition d,
                                                             UpdateFunctionCodeResult curFc) {
        List<String> returnedSecurityGroupIdsToMatch = Collections.emptyList();

        if (null != curFc.getVpcConfig() && !curFc.getVpcConfig().getSecurityGroupIds().isEmpty())
            returnedSecurityGroupIdsToMatch = curFc.getVpcConfig().getSecurityGroupIds();

        List<String> returnedSubnetIdsToMatch = Collections.emptyList();

        if (null != curFc.getVpcConfig() && !curFc.getVpcConfig().getSubnetIds().isEmpty())
            returnedSubnetIdsToMatch = curFc.getVpcConfig().getSubnetIds();

        boolean bEquals = new EqualsBuilder().
                append(d.getDescription(), curFc.getDescription()).
                append(d.getHandler(), curFc.getHandler()).
                append(d.getMemorySize(), curFc.getMemorySize().intValue()).
                append(d.getRole(), curFc.getRole()).
                append(d.getTimeout(), curFc.getTimeout().intValue()).
                append(this.securityGroupIds, returnedSecurityGroupIdsToMatch).
                append(this.subnetIds, returnedSubnetIdsToMatch).
                isEquals();

        if (!bEquals) {
            final UpdateFunctionConfigurationRequest
                    updRequest =
                    new UpdateFunctionConfigurationRequest();

            updRequest.setFunctionName(d.getName());
            updRequest.setDescription(d.getDescription());
            updRequest.setHandler(d.getHandler());
            updRequest.setMemorySize(d.getMemorySize());
            updRequest.setRole(d.getRole());
            updRequest.setTimeout(d.getTimeout());

            VpcConfig vpcConfig = new VpcConfig().
                    withSecurityGroupIds(this.securityGroupIds).
                    withSubnetIds(this.subnetIds);

            updRequest.setVpcConfig(vpcConfig);

            getLog().info(
                    format("Function Configuration doesn't match expected defaults. Updating it to %s.",
                            updRequest));

            final UpdateFunctionConfigurationResult
                    result =
                    lambdaClient.updateFunctionConfiguration(updRequest);

            return result;
        }

        return null;
    }

    private Map<String, LambdaFunctionDefinition> parseFunctionDefinions() throws Exception {
        String source = IOUtils.toString(new FileInputStream(definitionFile));

        // TODO: Consider PluginParameterExpressionEvaluator
        source = new StrSubstitutor(session.getSystemProperties()).replace(source);

        getLog()
                .info(format("Loaded and replaced definitions from file '%s'", definitionFile.getPath()));

        List<LambdaFunctionDefinition>
                definitionList =
                OBJECT_MAPPER.readValue(source, new TypeReference<List<LambdaFunctionDefinition>>() {
                });

        getLog().info(format("Found %d definitions: ", definitionList.size()));

        Map<String, LambdaFunctionDefinition> result = new TreeMap<String, LambdaFunctionDefinition>();

        for (LambdaFunctionDefinition d : definitionList) {
            if (0 == d.getMemorySize()) {
                d.setMemorySize(defaultMemorySize);
            }

            if (isBlank(d.getRole())) {
                d.setRole(defaultRole);
            } else {
                d.setRole(roleResolver.lookupRoleGlob(d.getRole()));
            }

            if (0 == d.getTimeout()) {
                d.setTimeout(defaultTimeout);
            }

            result.put(d.getName(), d);
        }

        {
            source = OBJECT_MAPPER.writeValueAsString(definitionList);

            getLog()
                    .debug(format("Parsed function definitions: %s", source));

            IOUtils.write(source, new FileOutputStream(definitionFile));
        }

        getLog().info(format("Merged into %d definitions: ", result.size()));

        return result;
    }

}

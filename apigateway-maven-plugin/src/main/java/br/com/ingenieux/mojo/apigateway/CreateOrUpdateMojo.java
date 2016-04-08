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

package br.com.ingenieux.mojo.apigateway;

import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.CreateRestApiRequest;
import com.amazonaws.services.apigateway.model.CreateRestApiResult;
import com.amazonaws.services.apigateway.model.PutMode;
import com.amazonaws.services.apigateway.model.PutRestApiRequest;
import com.amazonaws.services.apigateway.model.PutRestApiResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.Role;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.ingenieux.mojo.aws.util.RoleResolver;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

@Mojo(name = "create-or-update", requiresProject = true)
public class CreateOrUpdateMojo extends AbstractAPIGatewayMojo {
    /**
     * Regex to Find Parameter Keys (when importing)
     */
    public static final Pattern KEY_PARAM_REGEX = Pattern.compile("^apigateway\\.param\\.(.+)$");

    /**
     * Regex to Find Stage Parameters
     */
    public static final Pattern KEY_STAGE_VARIABLE_REGEX = Pattern.compile("^apigateway\\.stage\\.([^\\.]{3,}).(.+)$");

    /**
     * Rest API Description
     */
    @Parameter(property = "apigateway.restApiDescription", required = false, defaultValue = "API for ${project.artifactId}")
    protected String restApiDescription;

    /**
     * Stage Name Description
     */
    @Parameter(property = "apigateway.stageNameDescription", required = true, defaultValue = "${apigateway.stageName} stage")
    protected String stageNameDescription;

    /**
     * Stage Deployment Description
     */
    @Parameter(property = "apigateway.stageDeploymentDescription", required=true, defaultValue = "Updated by apigateway-maven-plugin")
    protected String deploymentDescription;

    /**
     * Deployment File to Use (Swagger)
     */
    @Parameter(property = "apigateway.deploymentFile", required=true, defaultValue = "${project.build.outputDirectory}/META-INF/apigateway/apigateway-swagger.json")
    protected File deploymentFile;

    /**
     * Cache Cluster Enabled (when creating a new stage)
     */
    @Parameter(property = "apigateway.cacheClusterEnabled", defaultValue = "false")
    protected Boolean cacheClusterEnabled;

    /**
     * Cache Cluster Size (when creating a new stage)
     */
    @Parameter(property = "apigateway.cacheClusterSize")
    protected String cacheClusterSize;

    /**
     * New Stage Definitions
     */
    @Parameter(required=false)
    protected Map<String, Map<String, String>> stageVariables = new LinkedHashMap<>();

    /**
     * Overwrite Definitions (otherwise, merge)
     */
    @Parameter(property = "apigateway.overwriteDefinitions", required=true, defaultValue = "false")
    protected Boolean overwriteDefinitions;

    /**
     * Parameters
     */
    @Parameter(property = "apigateway.parameters", required=false)
    protected Map<String, String> parameters = new HashMap<>();

    /**
     * Resulting Body to use
     */
    protected String body;

    private RoleResolver roleResolver;

    @Override
    protected Object executeInternal() throws Exception {
        this.roleResolver = new RoleResolver(createServiceFor(AmazonIdentityManagementClient.class));

        initProperties();

        createOrUpdateRestApi();

        loadAndInterpolateSwaggerFile();

        importDefinitions();

        cleanupPermissions();

        CreateDeploymentResult result = deploy();

        return result;
    }

    private CreateDeploymentResult deploy() {
        /**
         * Step #1: Doing the stage itself
         */
        CreateDeploymentRequest req = new CreateDeploymentRequest()
                .withRestApiId(restApiId)
                .withStageName(stageName)
                .withDescription(deploymentDescription)
                .withStageName(stageName)
                .withStageDescription(stageNameDescription)
                .withCacheClusterEnabled(cacheClusterEnabled)
                .withCacheClusterSize(cacheClusterSize)
                .withVariables(getStageVariables(stageName));

        getLog().info("Creating Deployment Request: " + req);

        CreateDeploymentResult result = getService().createDeployment(req);

        getLog().info("Deployment Request Result: " + result);

        return result;
    }

    private Map<String, String> getStageVariables(String stageName) {
        if (stageVariables.containsKey(stageName)) {
            return stageVariables.get(stageName);
        }

        return Collections.emptyMap();
    }

    private void cleanupPermissions() {

    }

    private void initProperties() {
        curProject.getProperties().entrySet().stream()
                .map(e -> KEY_PARAM_REGEX.matcher(e.getKey() + ""))
                .filter(m -> m.matches()).forEach(m -> {
            String k = m.group(1);
            String v = curProject.getProperties().getProperty(m.group(0), "");

            if (isEmpty(v) && parameters.containsKey(k)) {
                getLog().info("Removing parameter " + k);

                parameters.remove(k);

            } else {
                getLog().info("Updating parameter " + k + ": " + v);

                parameters.put(k, v);
            }
        });

        curProject.getProperties().entrySet().stream()
                .map(e -> KEY_STAGE_VARIABLE_REGEX.matcher(e.getKey() + ""))
                .filter(m -> m.matches()).forEach(m -> {
            String env = m.group(1);
            String k = m.group(2);
            String v = curProject.getProperties().getProperty(m.group(0), "");

            Map<String, String> stageVariablesForEnv = stageVariables.get(env);

            if (null == stageVariablesForEnv) {
                stageVariablesForEnv = new LinkedHashMap<String, String>();

                stageVariables.put(env, stageVariablesForEnv);
            }

            if (isEmpty(v) && stageVariablesForEnv.containsKey(k)) {
                getLog().info("Removing stage variable " + k);

                stageVariablesForEnv.remove(k);
            } else {
                getLog().info("Updating stage variable " + k + ": " + v);

                stageVariablesForEnv.put(k, v);
            }
        });
    }

    private PutRestApiResult importDefinitions() {
        getLog().info("Uploading definitions update (overwrite mode?: " + overwriteDefinitions + ")");

        final PutRestApiResult result = getService().putRestApi(new PutRestApiRequest()
                .withRestApiId(restApiId)
                .withMode(this.overwriteDefinitions ? PutMode.Overwrite : PutMode.Merge)
                .withBody(ByteBuffer.wrap(body.getBytes(DEFAULT_CHARSET)))
                .withParameters(parameters)
        );

        getLog().debug("result: " + result);

        return result;
    }

    private void loadAndInterpolateSwaggerFile() throws Exception {
        String accountId = roleResolver.getAccountId();
        String deploymentFileContents = IOUtils.toString(new FileInputStream(deploymentFile));

        getLog().info("Loaded deploymentFile contents from " + deploymentFile.getPath());

        deploymentFileContents = deploymentFileContents.replaceAll("\\Qarn:aws:iam:::role/\\E",
                "arn:aws:iam::" + accountId + ":role/");

        deploymentFileContents = deploymentFileContents.replaceAll("\\Qarn:aws:lambda:\\E[\\w\\-]*:\\d*:",
                "arn:aws:lambda:" + regionName + ":" + accountId + ":");

        getLog().debug("Contents: " + deploymentFileContents);

        ObjectNode objectNode = ObjectNode.class.cast(this.objectMapper.readTree(deploymentFileContents));

        this.body = objectMapper.writeValueAsString(objectNode);

        getLog().debug("Final body content: " + this.body);
    }

    private void createOrUpdateRestApi() throws Exception {
        super.lookupIds();

        if (isBlank(restApiId)) {
            CreateRestApiRequest req = new CreateRestApiRequest();

            req.withName(restApiName);
            req.withDescription(defaultString(restApiDescription));

            final CreateRestApiResult result = getService().createRestApi(req);

            getLog().info("Created restApi " + req + ": " + result);

            super.lookupIds();
        }
    }
}

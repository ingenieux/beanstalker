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

import com.google.common.collect.Lists;

import com.amazonaws.services.apigateway.model.CreateDeploymentRequest;
import com.amazonaws.services.apigateway.model.CreateDeploymentResult;
import com.amazonaws.services.apigateway.model.CreateRestApiRequest;
import com.amazonaws.services.apigateway.model.CreateRestApiResult;
import com.amazonaws.services.apigateway.model.PutMode;
import com.amazonaws.services.apigateway.model.PutRestApiRequest;
import com.amazonaws.services.apigateway.model.PutRestApiResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonPatch;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import br.com.ingenieux.mojo.apigateway.util.Unthrow;
import br.com.ingenieux.mojo.aws.util.RoleResolver;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

  private static final String STR_TEMPLATE_LAMBDA_METHOD = loadResourceAsString("lambda-method.json");

  private static final String STR_TEMPLATE_CORS_METHOD =
    loadResourceAsString("cors-method.json");

  private static String loadResourceAsString(String name) {
    try {
      return IOUtils.toString(CreateOrUpdateMojo.class.getClassLoader().getResource("templates/" + name));
    } catch (Exception exc) {
      throw new IllegalStateException("While loading template: " + name, exc);
    }
  }

  private static final Pattern PATTERN_PARAMETER = Pattern.compile("\\{(\\w+)\\}");

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
  @Parameter(property = "apigateway.stageDeploymentDescription", required = true, defaultValue = "Updated by apigateway-maven-plugin")
  protected String deploymentDescription;

  /**
   * Deployment File to Use (Swagger)
   */
  @Parameter(
    property = "apigateway.deploymentFile",
    required = true,
    defaultValue = "${project.build.outputDirectory}/META-INF/apigateway/apigateway-swagger.json"
  )
  protected File deploymentFile;

  /**
   * Deployment File to Use (Lambda)
   */
  @Parameter(property = "apigateway.lambdasFile", defaultValue = "${project.build.outputDirectory}/META-INF/lambda-definitions.json")
  protected File lambdasFile;

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
  @Parameter(required = false)
  protected Map<String, Map<String, String>> stageVariables = new LinkedHashMap<>();

  /**
   * Overwrite Definitions (otherwise, merge)
   */
  @Parameter(property = "apigateway.overwriteDefinitions", required = true, defaultValue = "false")
  protected Boolean overwriteDefinitions;

  /**
   * Overwrite Definitions (otherwise, merge)
   */
  @Parameter(property = "apigateway.overwriteDefinitions", required = true, defaultValue = "false")
  protected Boolean skipInterpolate;

  /**
   * Parameters
   */
  @Parameter(property = "apigateway.parameters", required = false)
  protected Map<String, String> parameters = new HashMap<>();

  /**
   * Remove Conflicting Declarations? Disable for advanced usage
   */
  @Parameter(property = "apigateway.removeConflicting", required = true, defaultValue = "true")
  protected boolean removeConflicting;

  /**
   * Resulting Body to use
   */
  protected String body;

  private RoleResolver roleResolver;

  private List<LambdaDefinition> lambdaDefinitions = Collections.emptyList();

  private ObjectNode templateChildNode;

  private ObjectNode templateOptionsNode;

  private ObjectNode swaggerDefinition;

  @Override
  protected Object executeInternal() throws Exception {
    this.roleResolver = new RoleResolver(createServiceFor(AmazonIdentityManagementClient.class));

    initConstants();

    initProperties();

    loadLambdaDefinitions();

    createOrUpdateRestApi();

    loadAndInterpolateSwaggerFile();

    importDefinitions();

    cleanupPermissions();

    CreateDeploymentResult result = deploy();

    return result;
  }

  private void initConstants() throws Exception {
    try {
      this.templateChildNode = (ObjectNode) objectMapper.readTree(STR_TEMPLATE_LAMBDA_METHOD);
    } catch (Exception exc) {
      getLog().warn("While building template credentials node:", exc);
    }

    try {
      this.templateChildNode.with("x-amazon-apigateway-integration").put("credentials", roleResolver.lookupRoleGlob("*/apigateway-lambda-invoker"));
    } catch (Exception exc) {
      getLog().warn("While building template credentials node:", exc);
    }

    try {
      this.templateOptionsNode = (ObjectNode) objectMapper.readTree(STR_TEMPLATE_CORS_METHOD);
    } catch (Exception exc) {
      getLog().warn("While building template credentials node:", exc);
    }
  }

  private CreateDeploymentResult deploy() {
    /**
     * Step #1: Doing the stage itself
     */
    CreateDeploymentRequest req =
      new CreateDeploymentRequest()
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
    final Properties props = getProperties();

    props
      .entrySet()
      .stream()
      .map(e -> KEY_PARAM_REGEX.matcher(e.getKey() + ""))
      .filter(m -> m.matches())
      .forEach(
        m -> {
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

    props
      .entrySet()
      .stream()
      .map(e -> KEY_STAGE_VARIABLE_REGEX.matcher(e.getKey() + ""))
      .filter(m -> m.matches())
      .forEach(
        m -> {
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

    if (props.containsKey("apigateway.restApiId")) {
      restApiId = props.getProperty("apigateway.restApiId");
    }

    if (props.containsKey("apigateway.restApiName")) {
      restApiName = props.getProperty("apigateway.restApiName");
    }

//    if (props.containsKey("apigateway.restApiHost")) {
//      restApiHost = props.getProperty("apigateway.restApiHost");
//    }
  }

  protected void loadLambdaDefinitions() throws Exception {
    if (null != lambdasFile && lambdasFile.exists()) {
      getLog().info("Loading lambdas from " + lambdasFile.getPath());

      Map<String, LambdaDefinition> defs = objectMapper.readValue(lambdasFile, new TypeReference<Map<String, LambdaDefinition>>() {
      });

      this.lambdaDefinitions = defs.values().stream().filter(x -> null != x.getApi()).collect(Collectors.toList());

      this.lambdaDefinitions.forEach(x -> x.getApi().methodType = x.getApi().methodType.toLowerCase());
    }
  }

  private PutRestApiResult importDefinitions() {
    getLog().info("Uploading definitions update (overwrite mode?: " + overwriteDefinitions + ")");

    final PutRestApiResult result =
      getService()
        .putRestApi(
          new PutRestApiRequest()
            .withRestApiId(restApiId)
            .withMode(this.overwriteDefinitions ? PutMode.Overwrite : PutMode.Merge)
            .withBody(ByteBuffer.wrap(body.getBytes(DEFAULT_CHARSET)))
            .withParameters(parameters));

    getLog().debug("result: " + result);

    return result;
  }

  private void loadAndInterpolateSwaggerFile() throws Exception {
    String accountId = roleResolver.getAccountId();
    String deploymentFileContents = IOUtils.toString(new FileInputStream(deploymentFile));

    boolean bYamlFile = (deploymentFile.getName().endsWith(".yaml"));

    getLog().info("Loaded deploymentFile contents from " + deploymentFile.getPath());

    // TODO: Consider PluginParameterExpressionEvaluator
    deploymentFileContents = new StrSubstitutor(getProperties()).replace(deploymentFileContents);

    // IMPROVE THIS
    if (!skipInterpolate) {
      deploymentFileContents = deploymentFileContents.replaceAll("\\Qarn:aws:iam:::role/\\E", "arn:aws:iam::" + accountId + ":role/");

      deploymentFileContents =
        deploymentFileContents.replaceAll("\\Qarn:aws:lambda:\\E[\\w\\-]*:\\d*:", "arn:aws:lambda:" + regionName + ":" + accountId + ":");
    }

    getLog().debug("Contents: " + deploymentFileContents);

    swaggerDefinition = (ObjectNode) (bYamlFile ? YAML_OBJECT_MAPPER : objectMapper).readTree(deploymentFileContents);

    swaggerDefinition.with("info").put("title", restApiName).put("description", restApiDescription);

    mergeLambdas(swaggerDefinition);

    ObjectNode objectNode = ObjectNode.class.cast(swaggerDefinition);

    this.body = objectMapper.writeValueAsString(objectNode);

    getLog().debug("Final body content: " + this.body);
  }

  protected void mergeLambdas(ObjectNode swaggerDefinition) throws Exception {
    getLog().info("Loaded " + lambdaDefinitions.size() + " active lambda definitions.");

    if (lambdaDefinitions.isEmpty() || skipInterpolate) {
      getLog().info("Skipping interpolation.");

      return;
    }

    ObjectNode pathNode = swaggerDefinition.with("paths");

    for (LambdaDefinition d : lambdaDefinitions) {
      removeConflictingDeclarations(d, pathNode);

      ObjectNode parentNode = templateChildNode.deepCopy();

      parentNode.with("x-amazon-apigateway-integration").put("httpMethod", "POST").put("uri", getUriFor(d));

      final ArrayNode parametersNode = parentNode.putArray("parameters");

      for (String parameterName : findParametersFor(d.getApi().getPath())) {
        ObjectNode parameterNode = objectMapper.createObjectNode();

        parameterNode.put("name", parameterName);
        parameterNode.put("in", "path");
        parameterNode.put("required", true);
        parameterNode.put("type", "string");

        parametersNode.add(parameterNode);
      }

      ObjectNode result = parentNode;

      {
        ArrayNode patches = getPatches(d.getApi());

        if (null != patches) {
          result = (ObjectNode) JsonPatch.apply(patches, parentNode);
        }
      }

      pathNode.with(d.getApi().getPath()).with(d.getApi().getMethodType()).removeAll();

      pathNode.with(d.getApi().getPath()).with(d.getApi().getMethodType()).setAll(result);
    }

    Map<String, Set<String>> corsPaths =
      lambdaDefinitions
        .stream()
        .filter(x -> x.api.isCorsEnabled())
        .map(x -> x.getApi())
        .collect(groupingBy(x -> x.getPath(), mapping(k -> k.getMethodType(), toSet())));

    for (Map.Entry<String, Set<String>> e : corsPaths.entrySet()) {
      String supportedMethods = format("'%s'", StringUtils.join(e.getValue().iterator(), ","));

      ObjectNode optionsNode = pathNode.with(e.getKey()).with("options");

      optionsNode.setAll(templateOptionsNode);

      optionsNode
        .with("x-amazon-apigateway-integration")
        .with("responses")
        .with("default")
        .with("responseParameters")
        .put("method.response.header.Access-Control-Allow-Methods", supportedMethods);
    }
  }

  private ArrayNode getPatches(LambdaDefinition.Api api) {
    if (null == api.getPatches() || 0 == api.getPatches().length) return null;

    ArrayNode result = objectMapper.createArrayNode();

    result.addAll(
      Arrays.stream(api.getPatches())
        .map(
          p ->
            Unthrow.wrap(
              patch -> {
                ObjectNode resultNode = objectMapper.createObjectNode();

                resultNode.put("op", patch.getOp().toLowerCase());

                resultNode.put("path", patch.getPath());

                // TODO: Interpolate patch.value
                if (isNotBlank(patch.getValue())) {
                  JsonNode value = null;

                  String sourceValue = patch.getValue();

                  if (-1 != "[{\"".indexOf(sourceValue.charAt(0))) {
                    value = objectMapper.readTree(sourceValue);
                  } else {
                    value = resultNode.textNode(sourceValue);
                  }

                  resultNode.set("value", value);
                } else if (isNotBlank(patch.getFrom())) {
                  JsonNode value = null;

                  String sourceValue = patch.getFrom();

                  if (-1 != "[{\"".indexOf(sourceValue.charAt(0))) {
                    value = objectMapper.readTree(sourceValue);
                  } else {
                    value = resultNode.textNode(sourceValue);
                  }

                  resultNode.set("from", value);
                }

                return resultNode;
              },
              p))
        .collect(Collectors.toList()));

    return result;
  }

  private String getUriFor(LambdaDefinition d) {
    // "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1::function:do_validateBot/invocations"

    return format("arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/%s/invocations", d.getArn());
  }

  private void removeConflictingDeclarations(LambdaDefinition d, ObjectNode pathNode) {
    if (!removeConflicting) return;
    String normalizedPath = normalizePath(d.getApi().getPath());

    outer:
    do {
      for (String path : Lists.newArrayList(pathNode.fieldNames())) {
        String normalizedExistingPath = normalizePath(path);

        boolean hasMatchingPath = normalizedExistingPath.equals(normalizedPath);

        if (hasMatchingPath) {
          getLog().info("Renaming possibly conflicting path " + path + " to " + d.getApi().getPath() + " (overrides will apply)");

          ObjectNode childNodesFromExisting = pathNode.with(path);

          pathNode.with(d.getApi().getPath()).setAll(childNodesFromExisting);

          pathNode.remove(path);
          continue outer;
        }
      }
    } while (false);
  }

  private String normalizePath(String path) {
    return path.replaceAll("\\{\\w+\\}", "{}");
  }

  protected Set<String> findParametersFor(String path) {
    Set<String> result = new LinkedHashSet<>();
    Matcher m = PATTERN_PARAMETER.matcher(path);

    while (m.find()) {
      result.add(m.group(1));
    }

    return result;
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

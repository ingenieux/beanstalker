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

import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.RestApi;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Optional;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

import static com.fasterxml.jackson.core.JsonParser.Feature;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class AbstractAPIGatewayMojo extends AbstractAWSMojo<AmazonApiGatewayClient> {
  protected static final ObjectMapper YAML_OBJECT_MAPPER =
      new ObjectMapper(
              new YAMLFactory().enable(Feature.ALLOW_COMMENTS).enable(Feature.ALLOW_YAML_COMMENTS).enable(JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION))
          .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          //https://github.com/ingenieux/beanstalker/issues/87 - Decouple the serialization of AWS responses to avoid warning mgs
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

  @Parameter(property = "project", required = true)
  protected MavenProject curProject;

  @Parameter(property = "apigateway.restApiName", required = true, defaultValue = "${project.artifactId}")
  protected String restApiName;

  /**
   * Derived from restApiName
   */
  protected String restApiId;

  @Parameter(property = "apigateway.stageName", required = true, defaultValue = "dev")
  protected String stageName;

  /**
   * Endpoint URL - Internal Usage.
   */
  protected String endpointUrl;

  protected void lookupIds() throws Exception {
    if (null != restApiId) {
      updateEndpoint();

      return;
    }

    GetRestApisRequest req = new GetRestApisRequest();
    String position = "";

    do {
      final GetRestApisResult apiList = getService().getRestApis(req);

      final Optional<RestApi> api = apiList.getItems().stream().filter(restApi -> restApi.getName().equals(restApiName)).findFirst();

      if (api.isPresent()) {
        getLog().info("Using api: " + api.get());

        this.restApiId = api.get().getId();

        break;
      }

      position = apiList.getPosition();

      req.setPosition(position);
    } while (isNotBlank(position));

    if (isBlank(restApiId)) return;

    updateEndpoint();
  }

  protected void updateEndpoint() {
    String propertyName = "apigateway.endpoint.url";
    String propertyValue = format("https://%s.execute-api.%s.amazonaws.com/%s", restApiId, regionName, stageName);

    getLog().info("Setting property: " + propertyName + "=" + propertyValue);

    session.getSystemProperties().put(propertyName, propertyValue);

    this.endpointUrl = propertyValue;
  }
}

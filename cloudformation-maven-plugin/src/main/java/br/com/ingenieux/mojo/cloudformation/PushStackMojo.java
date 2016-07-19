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
 *
 */

package br.com.ingenieux.mojo.cloudformation;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateResult;
import com.amazonaws.services.s3.AmazonS3URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;
import br.com.ingenieux.mojo.cloudformation.cmd.WaitForStackCommand;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Pushes (creates/updates) a given stack
 */
@Mojo(name = "push-stack")
public class PushStackMojo extends AbstractCloudformationMojo {
  @Parameter(
    property = "cloudformation.stackLocation",
    required = true,
    defaultValue = "${project.basedir}/src/main/cloudformation/${project.artifactId}.template.json"
  )
  File templateLocation;

  /**
   * If set to true, ignores/skips in case of a missing active stack found
   */
  @Parameter(property = "cloudformation.failIfMissing", defaultValue = "false")
  Boolean failIfMissing;

  /**
   * <p> S3 URL &quot;s3://&lt;bucketName&gt;/&lt;keyPath&gt;&quot; of the S3 Location </p>
   *
   * <p>If set, will upload the @{templateLocation} contents prior to issuing a stack
   * create/update process</p>
   */
  @Parameter(property = "cloudformation.s3Url")
  String s3Url;

  AmazonS3URI destinationS3Uri;

  /**
   * <p>Template Input Parameters</p>
   *
   * <p>On CLI usage, you can set <code>-Dcloudformation.paramters=ParamA=abc,ParamB=def</code></p>
   */
  @Parameter(property = "cloudformation.parameters")
  List<com.amazonaws.services.cloudformation.model.Parameter> parameters = new ArrayList<>();

  public void setParameters(String parameters) {
    List<String> nvPairs = asList(parameters.split(","));

    this.parameters =
        nvPairs
            .stream()
            .map(this::extractNVPair)
            .map(
                mapEntry ->
                    new com.amazonaws.services.cloudformation.model.Parameter().withParameterKey(mapEntry.getKey()).withParameterValue(mapEntry.getValue()))
            .collect(Collectors.toList());
  }

  /**
   * Notification ARNs
   */
  @Parameter List<String> notificationArns;

  /**
   * <p>On Failure</p>
   *
   * <p>Either &quot;DO_NOTHING&quot;, &quot;ROLLBACK&quot; or &quot;DELETE&quot;</p>
   */
  @Parameter(property = "cloudformation.onfailure", defaultValue = "DO_NOTHING")
  OnFailure onFailure = OnFailure.DO_NOTHING;

  /**
   * Resource Types
   */
  @Parameter Collection<String> resourceTypes = new ArrayList<>();

  /**
   * Disable Rollback?
   */
  @Parameter(defaultValue = "true")
  Boolean disableRollback = true;

  /**
   * Tags
   */
  @Parameter(property = "cloudformation.tags")
  List<Tag> tags = new ArrayList<>();

  public void setTags(String tags) {
    List<String> nvPairs = asList(tags.split(","));

    this.tags =
        nvPairs
            .stream()
            .map(this::extractNVPair)
            .map(mapEntry -> new Tag().withKey(mapEntry.getKey()).withValue(mapEntry.getValue()))
            .collect(Collectors.toList());
  }

  /**
   * Timeout in Minutes
   */
  @Parameter(property = "cloudformation.timeoutInMinutes")
  Integer timeoutInMinutes;

  BeanstalkerS3Client s3Client;
  private String templateBody;

  @Override
  protected Object executeInternal() throws Exception {
    shouldFailIfMissingStack(failIfMissing);

    if (!templateLocation.exists() && !templateLocation.isFile()) {
      getLog().warn("File not found (or not a file): " + templateLocation.getPath() + ". Skipping.");

      return null;
    }

    if (isNotBlank(s3Url)) {
      getLog().info("Uploading file " + this.templateLocation + " to location " + this.s3Url);

      s3Client = new BeanstalkerS3Client(getAWSCredentials(), getClientConfiguration(), getRegion());

      s3Client.setMultipartUpload(false);

      this.destinationS3Uri = new AmazonS3URI(s3Url);

      uploadContents(templateLocation, destinationS3Uri);
    } else {
      templateBody = IOUtils.toString(new FileInputStream(this.templateLocation));

      final Properties props = getProperties();

      templateBody = new StrSubstitutor(new StrLookup<String>() {
        @Override
        public String lookup(String key) {
          return (String) props.get(key);
        }
      }).replace(templateBody);
    }

    {
      ValidateTemplateResult validateTemplateResult = validateTemplate();

      if (!validateTemplateResult.getParameters().isEmpty()) {
        Set<String> existingParameterNames = this.parameters.stream().map(x -> x.getParameterKey()).collect(Collectors.toSet());

        Set<String> requiredParameterNames = validateTemplateResult.getParameters().stream().map(x -> x.getParameterKey()).collect(Collectors.toSet());

        for (String requiredParameter : requiredParameterNames) {
          if (!existingParameterNames.contains(requiredParameter)) {
            getLog().warn("Missing required parameter name: " + requiredParameter);
            getLog().warn("If its an update, will reuse previous value");
          }

          this.parameters.add(new com.amazonaws.services.cloudformation.model.Parameter().withParameterKey(requiredParameter).withUsePreviousValue(true));
        }
      }
    }

    WaitForStackCommand.WaitForStackContext ctx = null;

    Object result = null;

    if (null == stackSummary) {
      getLog().info("Must Create Stack");

      CreateStackResult createStackResult;
      result = createStackResult = createStack();

      ctx = new WaitForStackCommand.WaitForStackContext(createStackResult.getStackId(), getService(), this::info, 30, asList(StackStatus.CREATE_COMPLETE));

    } else {
      getLog().info("Must Update Stack");

      UpdateStackResult updateStackResult;

      result = updateStackResult = updateStack();

      if (null != result) {

        ctx = new WaitForStackCommand.WaitForStackContext(updateStackResult.getStackId(), getService(), this::info, 30, asList(StackStatus.UPDATE_COMPLETE));
      }
    }

    if (null != ctx) new WaitForStackCommand(ctx).execute();

    return result;
  }

  private ValidateTemplateResult validateTemplate() throws Exception {
    final ValidateTemplateRequest req = new ValidateTemplateRequest();

    if (null != destinationS3Uri) {
      req.withTemplateURL(generateExternalUrl(this.destinationS3Uri));
    } else {
      req.withTemplateBody(templateBody);
    }

    final ValidateTemplateResult result = getService().validateTemplate(req);

    getLog().info("Validation Result: " + result);

    return result;
  }

  private CreateStackResult createStack() throws Exception {
    CreateStackRequest req = new CreateStackRequest().withStackName(stackName).withCapabilities(Capability.CAPABILITY_IAM);

    if (null != this.destinationS3Uri) {
      req.withTemplateURL(generateExternalUrl(this.destinationS3Uri));
    } else {
      req.withTemplateBody(templateBody);
    }

    req.withNotificationARNs(notificationArns);

    req.withParameters(parameters);
    req.withResourceTypes(resourceTypes);
    req.withDisableRollback(disableRollback);
    req.withTags(tags);
    req.withTimeoutInMinutes(timeoutInMinutes);

    return getService().createStack(req);
  }

  protected String generateExternalUrl(AmazonS3URI destinationS3Uri) throws Exception {
    return s3Client.getResourceUrl(destinationS3Uri.getBucket(), destinationS3Uri.getKey());
  }

  private UpdateStackResult updateStack() throws Exception {
    UpdateStackRequest req = new UpdateStackRequest().withStackName(stackName).withCapabilities(Capability.CAPABILITY_IAM);

    if (null != this.destinationS3Uri) {
      req.withTemplateURL(generateExternalUrl(this.destinationS3Uri));
    } else {
      req.withTemplateBody(templateBody);
    }

    req.withNotificationARNs(notificationArns);

    req.withParameters(parameters);
    req.withResourceTypes(resourceTypes);
    req.withTags(tags);

    try {
      return getService().updateStack(req);
    } catch (AmazonServiceException exc) {
      if ("No updates are to be performed.".equals(exc.getErrorMessage())) {
        return null;
      }

      throw exc;
    }
  }

  private void uploadContents(File templateLocation, AmazonS3URI destinationS3Uri) throws Exception {
    s3Client.putObject(destinationS3Uri.getBucket(), destinationS3Uri.getKey(), new FileInputStream(this.templateLocation), null);
  }
}

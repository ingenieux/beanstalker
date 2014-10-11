package br.com.ingenieux.mojo.beanstalk.cmd.env.create;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;

import java.util.Arrays;

import br.com.ingenieux.mojo.aws.util.CredentialsUtil;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class CreateEnvironmentCommand extends
                                      BaseCommand<CreateEnvironmentContext, CreateEnvironmentResult> {

  /**
   * Constructor
   *
   * @param parentMojo parent mojo
   */
  public CreateEnvironmentCommand(AbstractBeanstalkMojo parentMojo)
      throws AbstractMojoExecutionException {
    super(parentMojo);
  }

  @Override
  protected CreateEnvironmentResult executeInternal(
      CreateEnvironmentContext context) throws Exception {
    CreateEnvironmentRequest request = new CreateEnvironmentRequest();

    request.setApplicationName(context.getApplicationName());
    request.setCNAMEPrefix(parentMojo.ensureSuffixStripped(context.getCnamePrefix()));
    request.setDescription(context.getApplicationDescription());
    request.setEnvironmentName(context.getEnvironmentName());

    request.setOptionSettings(Arrays.asList(context.getOptionSettings()));

    if ("Worker".equals(context.getEnvironmentTierName())) {
      if (contextDoesNotContainsEC2Role(context)) {
        parentMojo.getLog().warn(
            "It is meaningless to launch a worker without an IAM Role. If you set in templateName, thats fine, but here's a warning for you");
      }
      ;
      context.setEnvironmentTierType("SQS/HTTP");
      request.setCNAMEPrefix(null);
      request.setTier(new EnvironmentTier().withName(context.getEnvironmentTierName())
                          .withType(context.getEnvironmentTierType())
                          .withVersion(context.getEnvironmentTierVersion()));
    }

    if (StringUtils.isNotBlank(context.getTemplateName())) {
      request.setTemplateName(parentMojo.lookupTemplateName(
          context.getApplicationName(), context.getTemplateName()));
    } else if (StringUtils.isNotBlank(context.getSolutionStack())) {
      request.setSolutionStackName(context.getSolutionStack());
    }

    request.setVersionLabel(context.getVersionLabel());

    if (parentMojo.isVerbose()) {
      parentMojo.getLog().info(
          "Requesting createEnvironment w/ request: "
          + CredentialsUtil.redact("" + request));
    }

    return service.createEnvironment(request);
  }

  protected boolean contextDoesNotContainsEC2Role(CreateEnvironmentContext context) {
    boolean found = false;

    for (ConfigurationOptionSetting opt : context.getOptionSettings()) {
      found =
          opt.getOptionName().equals("IamInstanceProfile") && opt.getNamespace()
              .equals("aws:autoscaling:launchconfiguration");

      if (found) {
        break;
      }
    }

    return !found;
  }
}

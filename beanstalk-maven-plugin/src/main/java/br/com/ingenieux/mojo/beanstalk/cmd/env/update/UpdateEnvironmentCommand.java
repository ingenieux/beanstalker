package br.com.ingenieux.mojo.beanstalk.cmd.env.update;

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentResult;

import org.apache.maven.plugin.AbstractMojoExecutionException;

import java.util.Arrays;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;

import static org.apache.commons.lang.StringUtils.isNotBlank;

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
public class UpdateEnvironmentCommand extends
                                      BaseCommand<UpdateEnvironmentContext, UpdateEnvironmentResult> {

  /**
   * Constructor
   *
   * @param parentMojo parent mojo
   */
  public UpdateEnvironmentCommand(AbstractBeanstalkMojo parentMojo)
      throws AbstractMojoExecutionException {
    super(parentMojo);
  }

  @Override
  protected UpdateEnvironmentResult executeInternal(
      UpdateEnvironmentContext context) throws Exception {
    UpdateEnvironmentRequest req = new UpdateEnvironmentRequest();

    if (null != context.environmentDescription) {
      req.setDescription(context.environmentDescription);
    }

    if (null != context.environmentName) {
      req.setEnvironmentName(context.environmentName);
    } else if (null != context.environmentId) {
      req.setEnvironmentId(context.environmentId);
    }

    if (null != context.getEnvironmentTierName()) {
      String envTierType = "Standard";
      String envTierVersion = "1.0";

      if ("Worker".equals(context.getEnvironmentTierName())) {
        envTierType = "SQS/JSON";
      }

      req.setTier(
          new EnvironmentTier().withName(context.getEnvironmentTierName()).withType(envTierType)
              .withVersion(envTierVersion));
    }

    if (null != context.optionSettings && 0 != context.optionSettings.length) {
      req.setOptionSettings(Arrays.asList(context.optionSettings));
    }

    if (isNotBlank(context.versionLabel)) {
      info("Calling update-environment, and using versionLabel: " + context.versionLabel);

      req.setVersionLabel(context.versionLabel);
    } else if (isNotBlank(context.templateName)) {
      info("Calling update-environment, and using templateName: " + context.templateName);

      req.setTemplateName(context.templateName);
    }

    return service.updateEnvironment(req);
  }
}

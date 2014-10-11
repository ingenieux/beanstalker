package br.com.ingenieux.mojo.beanstalk.env;

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

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Updates the environment configuration (optionsSettings / optionsToRemove)
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html"
 * >UpdateEnvironment API</a> call.
 *
 * @since 0.2.2
 */
@Mojo(name = "update-environment-options", requiresDirectInvocation = true)
public class UpdateEnvironmentOptionsMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * Configuration Option Settings
   */
  @Parameter
  ConfigurationOptionSetting[] optionSettings;
  /**
   * Environment Name
   */
  @Parameter(property = "beanstalk.environmentDescription", defaultValue = "default")
  String environmentDescription;
  /**
   * Version Label to use.
   */
  @Parameter(property = "beanstalk.versionLabel")
  String versionLabel;
  /**
   * <p>Template Name.</p>
   *
   * <p>Could be either literal or a glob, like, <pre>ingenieux-services-prod-*</pre>. If a glob,
   * there will
   * be a lookup involved, and the first one in reverse ASCIIbetical order will be picked upon.
   * </p>
   */
  @Parameter(property = "beanstalk.templateName")
  String templateName;
  /**
   * What to set?
   */
  @Parameter(property = "beanstalk.whatToSet", defaultValue = "versionLabel", required = true)
  WhatToSet whatToSet;

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    UpdateEnvironmentRequest req = new UpdateEnvironmentRequest();

    req.setEnvironmentId(curEnv.getEnvironmentId());
    req.setEnvironmentName(curEnv.getEnvironmentName());

    if (WhatToSet.versionLabel.equals(whatToSet)) {
      req.setVersionLabel(versionLabel);
    } else if (WhatToSet.description.equals(whatToSet)) {
      req.setDescription(environmentDescription);
    } else if (WhatToSet.optionSettings.equals(whatToSet)) {
      req.setOptionSettings(getOptionSettings(optionSettings));
    } else if (WhatToSet.templateName.equals(whatToSet)) {
      req.setTemplateName(lookupTemplateName(applicationName, templateName));
      // } else if (WhatToSet.optionsToRemove.equals(whatToSet)) {
      // req.setOptionsToRemove(optionsToRemove)
    }

    return getService().updateEnvironment(req);
  }

  public enum WhatToSet {
    description, optionSettings, templateName, versionLabel
  }
}

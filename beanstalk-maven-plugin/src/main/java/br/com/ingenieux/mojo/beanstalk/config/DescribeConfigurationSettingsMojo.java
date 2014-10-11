package br.com.ingenieux.mojo.beanstalk.config;

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

import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Collection;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Returns the Configuration Settings
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeConfigurationSettings.html"
 * >DescribeConfigurationSettings API</a> call.
 *
 * @since 0.2.0
 */
@Mojo(name = "describe-configuration-settings")
public class DescribeConfigurationSettingsMojo extends
                                               AbstractNeedsEnvironmentMojo {

  /**
   * Template Name
   */
  @Parameter(property = "beanstalk.templateName")
  String templateName;

  @Override
  protected EnvironmentDescription handleResults(Collection<EnvironmentDescription> environments)
      throws MojoExecutionException {
    try {
      return super.handleResults(environments);
    } catch (Exception exc) {
      // Don't care - We're an exception to the rule, you know.

      return null;
    }
  }

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    boolean bTemplateNameDefined = isNotBlank(templateName) && !hasWildcards(templateName);

    DescribeConfigurationSettingsRequest
        req =
        new DescribeConfigurationSettingsRequest().withApplicationName(applicationName);

    if (bTemplateNameDefined) {
      req.withTemplateName(templateName);
    } else if (null != curEnv) {
      req.withEnvironmentName(curEnv.getEnvironmentName());
    } else {
      getLog().warn("You must supply a templateName or environmentName. Ignoring");

      return null;
    }

    getLog().info("Request: " + req);

    return getService().describeConfigurationSettings(req);
  }
}

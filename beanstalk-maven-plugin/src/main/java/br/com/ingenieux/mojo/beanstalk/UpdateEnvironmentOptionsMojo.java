package br.com.ingenieux.mojo.beanstalk;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

/**
 * Updates the environment configuration (optionsSettings / optionsToRemove)
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html"
 * >UpdateEnvironment API</a> call.
 * 
 * @goal update-environment-options
 */
public class UpdateEnvironmentOptionsMojo extends AbstractBeanstalkMojo {
	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentName}"
	 *            default-value="default"
	 */
	String environmentName;

	/**
	 * Environment Id
	 * 
	 * @parameter expression="${beanstalk.environmentId}"
	 */
	String environmentId;

	/**
	 * Configuration Option Settings
	 * 
	 * @parameter
	 */
	ConfigurationOptionSetting[] optionSettings;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentDescription}"
	 *            default-value="default"
	 * @required
	 */
	String environmentDescription;

	/**
	 * Version Label to use. Defaults to Project Version
	 * 
	 * @parameter expression="${beanstalk.versionLabel}"
	 *            default-value="${project.version}"
	 */
	String versionLabel;

	/**
	 * Template Name
	 * 
	 * @parameter expression="${beanstalk.templateName}"
	 */
	String templateName;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		UpdateEnvironmentRequest req = new UpdateEnvironmentRequest();

		req.setDescription(environmentDescription);
		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);
		req.setOptionSettings(getOptionSettings(optionSettings));
		req.setTemplateName(templateName);
		req.setVersionLabel(versionLabel);

		return service.updateEnvironment(req);
	}
}

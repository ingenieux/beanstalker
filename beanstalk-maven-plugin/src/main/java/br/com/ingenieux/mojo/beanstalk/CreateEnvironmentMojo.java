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
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;

/**
 * Creates and Launches an Elastic Beanstalk Environment
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html"
 * >CreateEnvironment API</a> call.
 * 
 * @goal create-environment
 */
public class CreateEnvironmentMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${beanstalk.applicationName}"
	 *            default-value="${project.artifactId}"
	 * @required
	 */
	String applicationName;

	/**
	 * DNS CName Prefix
	 * 
	 * @parameter expression="${beanstalk.cnamePrefix}"
	 *            default-value="${project.artifactId}"
	 */
	String cnamePrefix;

	/**
	 * Application Description
	 * 
	 * @parameter expression="${beanstalk.applicationDescription}"
	 *            default-value="${project.name}"
	 */
	String applicationDescription;

	/**
	 * Configuration Option Settings
	 * 
	 * @parameter
	 */
	ConfigurationOptionSetting[] optionSettings;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentName}"
	 *            default-value="default"
	 * @required
	 */
	String environmentName;

	/**
	 * Version Label to use. Defaults to Project Version
	 * 
	 * @parameter expression="${beanstalk.versionLabel}"
	 *            default-value="${project.version}"
	 */
	String versionLabel;

	/**
	 * Solution Stack Name
	 * 
	 * @parameter expression="${beanstalk.solutionStack}"
	 *            default-value="32bit Amazon Linux running Tomcat 7"
	 */
	String solutionStack;

	/**
	 * Template Name
	 * 
	 * @parameter expression="${beanstalk.templateName}"
	 */
	String templateName;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		CreateEnvironmentRequest request = new CreateEnvironmentRequest();

		request.setApplicationName(applicationName);
		request.setCNAMEPrefix(cnamePrefix);
		request.setDescription(applicationDescription);
		request.setEnvironmentName(environmentName);

		request.setOptionSettings(getOptionSettings(optionSettings));

		request.setSolutionStackName(solutionStack);

		request.setTemplateName(templateName);

		request.setVersionLabel(versionLabel);

		return service.createEnvironment(request);
	}
}

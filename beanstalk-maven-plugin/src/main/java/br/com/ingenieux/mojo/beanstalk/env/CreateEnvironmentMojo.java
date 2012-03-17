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

import org.apache.maven.plugin.AbstractMojoExecutionException;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentContextBuilder;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;

/**
 * Creates and Launches an Elastic Beanstalk Environment
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html"
 * >CreateEnvironment API</a> call.
 * 
 * @goal create-environment
 * @since 0.1.0
 */
public class CreateEnvironmentMojo extends AbstractNeedsEnvironmentMojo {
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
	protected Object executeInternal() throws AbstractMojoExecutionException {
		CreateEnvironmentResult result = createEnvironment(cnamePrefix);

		return result;
	}

	protected CreateEnvironmentResult createEnvironment(String cnameToCreate)
	    throws AbstractMojoExecutionException {
		String newEnvironmentName = getEnvironmentName(environmentName);

		CreateEnvironmentContextBuilder builder = CreateEnvironmentContextBuilder
		    .createEnvironmentContext() //
		    .withApplicationName(applicationName)//
		    .withApplicationDescription(applicationDescription)//
		    .withCnamePrefix(cnameToCreate)//
		    .withSolutionStack(solutionStack)//
		    .withTemplateName(templateName)//
		    .withEnvironmentName(newEnvironmentName)//
		    .withOptionSettings(optionSettings)//
		    .withVersionLabel(versionLabel);//
		
		CreateEnvironmentContext context = builder.build();

		CreateEnvironmentCommand command = new CreateEnvironmentCommand(this);

		return command.execute(context);
	}

	protected String getEnvironmentName(String environmentName) {
		return environmentName;
	}
}

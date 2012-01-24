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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification;

/**
 * Returns the Configuration Settings
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeConfigurationOptions.html"
 * >DescribeConfigurationOptions API</a> call.
 * 
 * @since 0.2.0
 * @goal describe-configuration-options
 */
public class DescribeConfigurationOptionsMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * Template Name
	 * 
	 * @parameter expression="${beanstalk.templateName}"
	 */
	String templateName;

	/**
	 * Solution Stack Name
	 * 
	 * @parameter expression="${beanstalk.solutionStack}"
	 *            default-value="32bit Amazon Linux running Tomcat 7"
	 */
	String solutionStack;

	/**
	 * Option Specifications
	 * 
	 * @parameter
	 */
	OptionSpecification[] optionSpecifications;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DescribeConfigurationOptionsRequest req = new DescribeConfigurationOptionsRequest()//
		    .withApplicationName(this.applicationName)//
		    .withEnvironmentName(environmentName)//
		    .withOptions(optionSpecifications)//
		    .withSolutionStackName(solutionStack)//
		    .withTemplateName(templateName)//
		;

		return service.describeConfigurationOptions(req);
	}
}

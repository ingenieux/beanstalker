package br.com.ingenieux.mojo.beanstalk;

import java.util.Collection;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

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

public abstract class AbstractNeedsEnvironmentMojo extends
    AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${beanstalk.applicationName}"
	 *            default-value="${project.artifactId}"
	 * @required
	 */
	protected String applicationName;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentName}"
	 */
	protected String environmentName;

	/**
	 * Environment Id
	 * 
	 * @parameter expression="${beanstalk.environmentId}"
	 */
	protected String environmentId;

	/**
	 * Default Environment Name
	 * 
	 * @parameter expression="${beanstalk.defaultEnvironmentName}"
	 *            default-value="default"
	 */
	protected String defaultEnvironmentName;

	@Override
	protected void configure() {
		boolean bNameDefined = org.apache.commons.lang.StringUtils
		    .isNotBlank(environmentName);
		boolean bIdDefined = org.apache.commons.lang.StringUtils
		    .isNotBlank(environmentId);

		if (bNameDefined ^ bIdDefined)
			return;

		if (bNameDefined == true && bIdDefined == true)
			return;

		getLog()
		    .info(
		        "environmentName / environmentId not defined. Lets try to get one, shall we?");

		Collection<EnvironmentDescription> environmentsFor = getEnvironmentsFor(applicationName);

		if (environmentsFor.isEmpty()) {
			getLog().info(
			    "No running environments found. Assigning defaultEnvironmentName");
			this.environmentName = defaultEnvironmentName;
		} else if (1 == environmentsFor.size()) {
			EnvironmentDescription env = environmentsFor.iterator().next();
			
			getLog().info(
			    "Assigning a environment named " + env.getEnvironmentName());
			
			//this.environmentId = envId;
			this.environmentName = env.getEnvironmentName();
		} else {
			getLog()
			    .info(
			        "Too many running environments found. Will not pick one. Declare -Dbeanstalk.environmentName next time.");
		}
	}

	/**
	 * Returns a list of environments for applicationName
	 * 
	 * @param applicationName
	 *          applicationName
	 * @return environments
	 */
	protected Collection<EnvironmentDescription> getEnvironmentsFor(
	    String applicationName) {
		/*
		 * Requests
		 */
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
		    .withApplicationName(applicationName).withIncludeDeleted(false);

		return service.describeEnvironments(req).getEnvironments();
	}
}

package br.com.ingenieux.mojo.beanstalk;

import java.util.Collection;

import org.jfrog.maven.annomojo.annotations.MojoParameter;

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
	@MojoParameter(expression="${beanstalk.applicationName}", defaultValue="${project.artifactId}", required=true, description="Beanstalk Application Name")
	protected String applicationName;

	@MojoParameter(expression="${beanstalk.environmentName}", description="Environment Name")
	protected String environmentName;

	@MojoParameter(expression="${beanstalk.environmentId}", description="Environment Id")
	protected String environmentId;

	@MojoParameter(expression="${beanstalk.defaultEnvironmentName}", defaultValue="default", description="Default Environment Name")
	protected String defaultEnvironmentName;
        
        @MojoParameter(expression="${beanstalk.cnamePrefix}", description = "")
        protected String cnamePrefix;

	@Override
	protected void configure() {
		boolean bNameDefined = org.apache.commons.lang.StringUtils
		    .isNotBlank(environmentName);
		boolean bIdDefined = org.apache.commons.lang.StringUtils
		    .isNotBlank(environmentId);
                boolean bCnameDefined = org.apache.commons.lang.StringUtils
                    .isNotBlank(cnamePrefix);

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
			if (bCnameDefined) {
                            if (!env.getEnvironmentName().startsWith(cnamePrefix)) {
                                
                                getLog().info(
                                        "Not assigning any {beanstalk.environmentName} since the only available environment doesn't match the {beanstalk.cnamePrefix}");
                                return;
                            }
                        }
			getLog().info(
			    "Assigning a environment named " + env.getEnvironmentName());
			
			//this.environmentId = envId;
			this.environmentName = env.getEnvironmentName();
		} else {
                        if (bCnameDefined) {
                            for (final EnvironmentDescription env : environmentsFor) {
                                final String cnameToMatch = cnamePrefix + ".elasticbeanstalk.com";
                                if (env.getCNAME().equalsIgnoreCase(cnameToMatch)) {
                                    this.environmentName = env.getEnvironmentName();
                                    this.environmentId = env.getEnvironmentId();
                                    
                                    getLog()
                                            .info(
                                                "Assigning a environment named " + env.getEnvironmentName() + " because it matched the 'cnamePrefix' = '" + cnamePrefix + "'");
                                    return;
                                }
                            }
                            getLog()
                                    .info("Unable to find a running environment matching cnamePrefix: " + cnamePrefix);
                            return;
                        }
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

		return getService().describeEnvironments(req).getEnvironments();
	}
}

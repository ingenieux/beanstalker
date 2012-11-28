package br.com.ingenieux.mojo.beanstalk;

import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
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

    @MojoParameter(expression="${beanstalk.cnamePrefix}", description = "cnamePrefix")
    protected String cnamePrefix;

    /**
     * Current Environment
     */
	protected EnvironmentDescription curEnv;

	@Override
	protected void configure() {
		try {
			curEnv = super.lookupEnvironment(applicationName, "", null, null, cnamePrefix);
		} catch (MojoExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a list of environments for current application name
	 * @param cnamePrefix cname prefix to match
	 * @return found environment, if any. null otherwise
	 */
	protected EnvironmentDescription getEnvironmentForCNamePrefix(
			String applicationName, String cnamePrefix) {
		for (final EnvironmentDescription env : getEnvironmentsFor(applicationName)) {
			final String cnameToMatch = cnamePrefix
					+ ".elasticbeanstalk.com";
			if (env.getCNAME().equalsIgnoreCase(cnameToMatch))
				return env;
		}
		
		return null;
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

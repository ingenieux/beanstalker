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

import com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest;

/**
 * Rebuilds an Environment
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RebuildEnvironment.html"
 * >RebuildEnvironment API</a> call.
 * 
 * @author Aldrin Leal
 * @goal rebuild-environment
 */
public class RebuildEnvironmentMojo extends AbstractBeanstalkMojo {
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

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		RebuildEnvironmentRequest req = new RebuildEnvironmentRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);

		service.rebuildEnvironment(req);

		return null;
	}

}

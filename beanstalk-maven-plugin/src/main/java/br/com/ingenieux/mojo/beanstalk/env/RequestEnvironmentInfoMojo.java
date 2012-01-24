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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.RequestEnvironmentInfoRequest;

/**
 * Returns Environment Info
 * 
 * See the docs for <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RequestEnvironmentInfo.html"
 * >RequestEnvironmentInfo API</a> call.
 * 
 * @author Aldrin Leal
 * @since 0.2.6
 * @goal request-environment-info
 * @requiresDirectInvocation
 */
public class RequestEnvironmentInfoMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * Type of information ro retrieve. Accepted: <code>tail</code>
	 * 
	 * @parameter expression="${beanstalk.infoType}" default-value="tail"
	 * @required
	 */
	private String infoType;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		RequestEnvironmentInfoRequest request = new RequestEnvironmentInfoRequest()
		    .withEnvironmentId(environmentId).withEnvironmentName(environmentName)
		    .withInfoType(infoType);

		service.requestEnvironmentInfo(request);

		return null;
	}
}

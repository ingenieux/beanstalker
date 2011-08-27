package br.com.ingenieux.mojo.beanstalk.dns;

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

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;

/**
 * Checks the availability of a CNAME.
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CheckDNSAvailability.html"
 * >CheckDNSAvailability API</a> call.
 * 
 * @goal check-availability
 * @requiresDirectInvocation
 */
public class CheckAvailabilityMojo extends AbstractBeanstalkMojo {
	/**
	 * DNS CName Prefix
	 * 
	 * @parameter expression="${beanstalk.cnamePrefix}"
	 *            default-value="${project.artifactId}"
	 */
	String cnamePrefix;

	/**
	 * Issue a failure when existing?
	 * 
	 * @parameter expression="${failWhenExists}"
	 */
	boolean failWhenExists = false;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		CheckDNSAvailabilityRequest checkDNSAvailabilityRequest = new CheckDNSAvailabilityRequest(
		    cnamePrefix);

		CheckDNSAvailabilityResult result = service
		    .checkDNSAvailability(checkDNSAvailabilityRequest);

		if (failWhenExists && !result.isAvailable())
			throw new MojoFailureException("CNAME exists: " + cnamePrefix);

		return result;
	}
}

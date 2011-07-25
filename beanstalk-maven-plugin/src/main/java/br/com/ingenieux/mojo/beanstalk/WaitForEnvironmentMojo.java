package br.com.ingenieux.mojo.beanstalk;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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

/**
 * Waits for Environment Status to Change
 * 
 * @goal wait-for-environment
 */
public class WaitForEnvironmentMojo extends AbstractBeanstalkMojo {
	private static final long MINUTE = 60 * 1000;

	/**
	 * Minutes until timeout
	 * 
	 * @parameter expression="${beanstalk.timeoutMins}" default-value="20"
	 */
	Integer timeoutMins;

	/**
	 * Status to Wait For
	 * 
	 * @parameter expression="${beanstalk.statusToWaitFor}" default-value="Ready"
	 */
	String statusToWaitFor;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		boolean done = false;
		Date expiresAt = new Date(System.currentTimeMillis() + MINUTE * timeoutMins);

		getLog().info("Will wait until " + expiresAt);

		do {
			if (timedOutP(expiresAt))
				throw new MojoExecutionException("Timed out");

			DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
			    .withEnvironmentNames(this.environmentName);

			DescribeEnvironmentsResult result = service.describeEnvironments(req);

			for (EnvironmentDescription d : result.getEnvironments()) {
				getLog().debug(
				    "Environment Detail:" + ToStringBuilder.reflectionToString(d));

				done = d.getStatus().equalsIgnoreCase(statusToWaitFor);

				if (done)
					return d;
			}
			sleepMinute();
		} while (true);
	}

	boolean timedOutP(Date expiresAt) throws MojoExecutionException {
		return expiresAt.before(new Date(System.currentTimeMillis()));
	}

	void sleepMinute() {
		try {
			Thread.sleep(MINUTE);
		} catch (InterruptedException e) {
		}
	}
}

package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.plugin.MojoExecutionException;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
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
public class WaitForEnvironmentCommand extends
    BaseCommand<WaitForEnvironmentContext, EnvironmentDescription> {
	private static final long INTERVAL = 15 * 1000;

	/**
	 * Constructor
	 * 
	 * @param parentMojo
	 *          parent mojo
	 */
	public WaitForEnvironmentCommand(AbstractBeanstalkMojo parentMojo) {
		super(parentMojo);
	}

	public EnvironmentDescription executeInternal(
	    WaitForEnvironmentContext context) throws Exception {
		long timeoutMins = context.getTimeoutMins();
		String environmentId = context.getEnvironmentId();
		String applicationName = context.getApplicationName();
		String statusToWaitFor = context.getStatusToWaitFor();

		boolean hasDomainToWaitFor = StringUtils.isNotBlank(context
		    .getDomainToWaitFor());

		String domainToWaitFor = String.format("%s.elasticbeanstalk.com",
		    context.getDomainToWaitFor());

		Date expiresAt = new Date(System.currentTimeMillis() + INTERVAL * timeoutMins);

		boolean done = false;

		info("Will wait until " + expiresAt + " for environment " + environmentId
		    + " to get into " + statusToWaitFor);

		if (hasDomainToWaitFor)
			info("... as well as having domain " + domainToWaitFor);

		do {
			if (timedOutP(expiresAt))
				throw new MojoExecutionException("Timed out");

			DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
			    .withApplicationName(applicationName).withEnvironmentIds(
			        environmentId);

			DescribeEnvironmentsResult result = service.describeEnvironments(req);

			for (EnvironmentDescription d : result.getEnvironments()) {
				debug("Environment Detail:" + ToStringBuilder.reflectionToString(d));

				done = d.getStatus().equalsIgnoreCase(statusToWaitFor);

				if (done && hasDomainToWaitFor)
					done = domainToWaitFor.equals(d.getCNAME());

				if (done)
					return d;
			}
			sleepInterval();
		} while (true);
	}

	boolean timedOutP(Date expiresAt) throws MojoExecutionException {
		return expiresAt.before(new Date(System.currentTimeMillis()));
	}

	void sleepInterval() {
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
		}
	}
}

package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import static java.lang.String.format;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.plugin.AbstractMojoExecutionException;
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
	
	/**
	 * Poll Interval
	 */
	public static final long POLL_INTERVAL = 90 * 1000;
	
	/**
	 * Magic Constant for Mins to MSEC
	 */
	private static final long MINS_TO_MSEC = 60 * 1000;

	/**
	 * Constructor
	 * 
	 * @param parentMojo
	 *          parent mojo
	 * @throws AbstractMojoExecutionException 
	 */
	public WaitForEnvironmentCommand(AbstractBeanstalkMojo parentMojo) throws AbstractMojoExecutionException {
		super(parentMojo);
	}

	public EnvironmentDescription executeInternal(
	    WaitForEnvironmentContext context) throws Exception {
		long timeoutMins = context.getTimeoutMins();
		String environmentId = context.getEnvironmentId();
		String applicationName = context.getApplicationName();
		String statusToWaitFor = context.getStatusToWaitFor();
		boolean negated = statusToWaitFor.startsWith("!");
		
		if (negated) {
			statusToWaitFor = statusToWaitFor.substring(1);
		}

		boolean hasDomainToWaitFor = StringUtils.isNotBlank(context
		    .getDomainToWaitFor());

		String domainToWaitFor = String.format("%s.elasticbeanstalk.com",
		    context.getDomainToWaitFor());

		Date expiresAt = new Date(System.currentTimeMillis() + MINS_TO_MSEC * timeoutMins);

		boolean done = false;

		info("Will wait until " + expiresAt + " for environment " + environmentId
		    + " to get into " + (negated ? "!" : "") + statusToWaitFor);

		if (hasDomainToWaitFor)
			info("... as well as having domain " + domainToWaitFor);

		do {
			if (timedOutP(expiresAt))
				throw new MojoExecutionException("Timed out");

			DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
			    .withApplicationName(applicationName).withEnvironmentIds(
			        environmentId);
			
			if (statusToWaitFor.startsWith("Terminat"))
				req.withIncludeDeleted(true);

			DescribeEnvironmentsResult result = service.describeEnvironments(req);
			
			boolean covered = false;

			for (EnvironmentDescription d : result.getEnvironments()) {
				info("Environment Detail:" + ToStringBuilder.reflectionToString(d));
				
				done = d.getStatus().equalsIgnoreCase(statusToWaitFor);
				
				if (negated)
					done = !done;
				
				covered |= d.getEnvironmentId().equals(environmentId);

				if (done && hasDomainToWaitFor)
					done = domainToWaitFor.equals(d.getCNAME());

				if (done)
					return d;
			}
			
			if (! covered && "Terminated".equals(statusToWaitFor)) {
				info(String.format("Environment id %s not even returned. Probably gone", environmentId));
				return null;
			}
			
			sleepInterval(POLL_INTERVAL);
		} while (true);
	}

	boolean timedOutP(Date expiresAt) throws MojoExecutionException {
		return expiresAt.before(new Date(System.currentTimeMillis()));
	}

	public void sleepInterval(long pollInterval) {
		logger.info(format("Sleeping for %d seconds", pollInterval / 1000));
		try {
			Thread.sleep(pollInterval);
		} catch (InterruptedException e) {
		}
	}
}

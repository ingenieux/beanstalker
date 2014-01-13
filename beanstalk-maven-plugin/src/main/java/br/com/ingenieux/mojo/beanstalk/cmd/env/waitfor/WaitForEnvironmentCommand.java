package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;
import com.amazonaws.services.elasticbeanstalk.model.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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

	static class EventDescriptionComparator implements
			Comparator<EventDescription> {
		@Override
		public int compare(EventDescription o1, EventDescription o2) {
			return o1.getEventDate().compareTo(o2.getEventDate());
		}
	}

	/**
	 * Poll Interval
	 */
	public static final long POLL_INTERVAL = 15 * 1000;

	/**
	 * Magic Constant for Mins to MSEC
	 */
	private static final long MINS_TO_MSEC = 60 * 1000;

	/**
	 * Constructor
	 * 
	 * @param parentMojo
	 *            parent mojo
	 * @throws AbstractMojoExecutionException
	 */
	public WaitForEnvironmentCommand(AbstractBeanstalkMojo parentMojo)
			throws AbstractMojoExecutionException {
		super(parentMojo);
	}

	public EnvironmentDescription executeInternal(
			WaitForEnvironmentContext context) throws Exception {
		long timeoutMins = context.getTimeoutMins();
		String environmentId = context.getEnvironmentId();
		String applicationName = context.getApplicationName();
		String statusToWaitFor = context.getStatusToWaitFor();
        String healthToWaitFor = context.getHealth();
        String environmentName = context.getEnvironmentName();
        boolean negated = statusToWaitFor.startsWith("!");

        if (isNotBlank(environmentName))
            context.setDomainToWaitFor(null);

		if (negated) {
			statusToWaitFor = statusToWaitFor.substring(1);
		}

		boolean hasDomainToWaitFor = isNotBlank(context
                .getDomainToWaitFor());

		String domainToWaitFor = null;

        if (isNotBlank(context.getDomainToWaitFor())) {
            domainToWaitFor = format("%s.elasticbeanstalk.com",
                    context.getDomainToWaitFor());
        }

		Date expiresAt = new Date(System.currentTimeMillis() + MINS_TO_MSEC
				* timeoutMins);
		Date lastMessageRecord = new Date();

		boolean done = false;

		info("Will wait until " + expiresAt + " to get into "
				+ (negated ? "!" : "") + statusToWaitFor);

		if (hasDomainToWaitFor)
			info("... as well as having domain " + domainToWaitFor);

		do {
			if (timedOutP(expiresAt))
				throw new MojoExecutionException("Timed out");

			DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
					.withApplicationName(applicationName)//
                    .withEnvironmentNames(environmentName)//
					.withEnvironmentIds(environmentId);

			if (statusToWaitFor.startsWith("Terminat"))
				req.withIncludeDeleted(true);

			DescribeEnvironmentsResult result = service
					.describeEnvironments(req);

			boolean covered = false;

			for (EnvironmentDescription d : result.getEnvironments()) {
				if (parentMojo.isVerbose())
					info("Environment Detail:"
							+ ToStringBuilder.reflectionToString(d));

				done = d.getStatus().equalsIgnoreCase(statusToWaitFor);

                if (done && isNotBlank(healthToWaitFor))
                    done = healthToWaitFor.equals(d.getHealth());

				if (negated)
					done = !done;

				covered |= d.getEnvironmentId().equals(environmentId);

				if (done && hasDomainToWaitFor)
					done = domainToWaitFor.equals(d.getCNAME());

				if (done)
					return d;
			}

			if (!covered && "Terminated".equals(statusToWaitFor)) {
				info(String.format(
						"Environment id %s not even returned. Probably gone",
						environmentId));
				return null;
			} else {
				DescribeEventsResult events = service
						.describeEvents(new DescribeEventsRequest()
								.withApplicationName(applicationName)
								.withStartTime(
										new Date(1000 + lastMessageRecord
												.getTime()))
								.withEnvironmentId(environmentId)
								.withSeverity("TRACE"));

				Set<EventDescription> eventList = new TreeSet<EventDescription>(
						new EventDescriptionComparator());

				eventList.addAll(events.getEvents());

				for (EventDescription d : eventList) {
					info(String.format("%s %s %s", d.getSeverity(),
							d.getEventDate(), d.getMessage()));

					lastMessageRecord = d.getEventDate();
				}
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

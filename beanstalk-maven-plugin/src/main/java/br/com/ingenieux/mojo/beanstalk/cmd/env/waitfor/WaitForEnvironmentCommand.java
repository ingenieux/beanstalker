package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

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
		// Those are invariants
		long timeoutMins = context.getTimeoutMins();
		String applicationName = context.getApplicationName();

		// as well as those (which are used as predicate variables, thus being
		// final)
		final String environmentId = context.getEnvironmentId();
		final String statusToWaitFor = context.getStatusToWaitFor();
		final String healthToWaitFor = context.getHealth();
		final String workerEnvironmentName = context.getWorkerEnvironmentName();

		// some argument juggling

		final boolean negated = statusToWaitFor.startsWith("!");

		if (isNotBlank(workerEnvironmentName))
			context.setDomainToWaitFor(null);

		Predicate<EnvironmentDescription> envPredicate = null;

		{
			// start building predicates with the status one - "![status]" must
			// be equal to status or not status
			final int offset = negated ? 1 : 0;
			final String vStatusToWaitFor = statusToWaitFor.substring(offset);

			envPredicate = new Predicate<EnvironmentDescription>() {
				public boolean apply(EnvironmentDescription t) {

					boolean result = vStatusToWaitFor.equals(t.getStatus());

					if (negated)
						result = !result;

					debug("testing status '%s' as equal as '%s' (negated? %s, offset: %d): %s",
							vStatusToWaitFor, t.getStatus(), negated, offset,
							result);

					return result;
				}
			};
			info("... with status %s set to '%s'", (negated ? "*NOT*" : " "), vStatusToWaitFor);
		}
		
		{
			if (isNotBlank(environmentId)) {
				envPredicate = Predicates.and(envPredicate, new Predicate<EnvironmentDescription>() { 
					@Override
					public boolean apply(EnvironmentDescription t) {
						return t.getEnvironmentId().equals(environmentId);
					}
				});
				
				info("... with environmentId equal to '%s'", environmentId);
			}
		}

		{
			if (isNotBlank(healthToWaitFor)) {
				envPredicate = Predicates.and(envPredicate, new Predicate<EnvironmentDescription>() { 
					@Override
					public boolean apply(EnvironmentDescription t) {
						return t.getHealth().equals(healthToWaitFor);
					}
				});
				
				info("... with health equal to '%s'", healthToWaitFor);
			}
		}

		{
			// then by cnamePrefix

			if (isNotBlank(workerEnvironmentName)) {
				envPredicate = Predicates.and(envPredicate,
						new Predicate<EnvironmentDescription>() {
							@Override
							public boolean apply(EnvironmentDescription t) {
								return workerEnvironmentName.equals(t
										.getEnvironmentName());
							}
						});
				info("... and with environmentName set to '%s'", workerEnvironmentName);
			} else if (isNotBlank(context.getDomainToWaitFor())) {
				// as well as by worker environment
				final String domainToWaitFor = format(
						"%s.elasticbeanstalk.com", context.getDomainToWaitFor());

				envPredicate = Predicates.and(envPredicate,
						new Predicate<EnvironmentDescription>() {
							@Override
							public boolean apply(EnvironmentDescription t) {
								return t.getCNAME().equals(domainToWaitFor);
							}
						});
				info("... and with cnamePrefix set to '%s'", domainToWaitFor);
			}
		}

		Date expiresAt = new Date(System.currentTimeMillis() + MINS_TO_MSEC
				* timeoutMins);
		Date lastMessageRecord = new Date();

		boolean includeDeletedP = statusToWaitFor.startsWith("Terminat");

		info("Will wait until " + expiresAt + " to get into expected condition");

		do {
			lastMessageRecord = displayEvents(applicationName,
					lastMessageRecord, environmentId);

			DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
					.withApplicationName(applicationName)//
					.withIncludeDeleted(includeDeletedP);

			DescribeEnvironmentsResult result = service
					.describeEnvironments(req);

			List<EnvironmentDescription> environments = result
					.getEnvironments();

			debug("There are %d environments", environments.size());

			for (EnvironmentDescription d : environments)
				debug("Testing environment %s: %s", d, envPredicate.apply(d));

			Collection<EnvironmentDescription> validEnvironments = Collections2
					.filter(environments, envPredicate);

			if (1 == validEnvironments.size()) {
				EnvironmentDescription foundEnvironment = validEnvironments.iterator()
						.next();
				
				debug("Found environment %s", foundEnvironment);

				return foundEnvironment;
			} else {
				debug("Found %d environments. No good. Ignoring.",
						validEnvironments.size());
				for (EnvironmentDescription d : validEnvironments)
					debug(" ... %s", d);
			}

			sleepInterval(POLL_INTERVAL);
		} while (!timedOutP(expiresAt));

		throw new MojoExecutionException("Timed out");
	}

	protected Date displayEvents(String applicationName,
			Date lastMessageRecord, String environmentId) {
		DescribeEventsResult events = service
				.describeEvents(new DescribeEventsRequest()
						.withApplicationName(applicationName)
						.withStartTime(
								new Date(1000 + lastMessageRecord.getTime()))
						.withEnvironmentId(environmentId).withSeverity("TRACE"));

		Set<EventDescription> eventList = new TreeSet<EventDescription>(
				new EventDescriptionComparator());

		eventList.addAll(events.getEvents());

		for (EventDescription d : eventList) {
			info(String.format("%s %s %s", d.getSeverity(), d.getEventDate(),
					d.getMessage()));

			lastMessageRecord = d.getEventDate();
		}

		return lastMessageRecord;
	}

	boolean timedOutP(Date expiresAt) throws MojoExecutionException {
		return expiresAt.before(new Date(System.currentTimeMillis()));
	}

	public void sleepInterval(long pollInterval) {
		debug("Sleeping for %d seconds", pollInterval / 1000);
		try {
			Thread.sleep(pollInterval);
		} catch (InterruptedException e) {
		}
	}
}

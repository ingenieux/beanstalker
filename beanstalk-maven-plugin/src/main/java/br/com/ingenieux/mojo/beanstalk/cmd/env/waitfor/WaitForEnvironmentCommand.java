package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;
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
	public static final long POLL_INTERVAL = 30 * 1000;

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
			throws MojoExecutionException {
		super(parentMojo);
	}

    public Collection<EnvironmentDescription> lookupInternal(WaitForEnvironmentContext context) {
        Predicate<EnvironmentDescription> envPredicate = getEnvironmentDescriptionPredicate(context);

        DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().withApplicationName(context.getApplicationName()).withIncludeDeleted(true);

        final List<EnvironmentDescription> envs = parentMojo.getService().describeEnvironments(req).getEnvironments();

        return Collections2.filter(envs, envPredicate);
    }

    protected Predicate<EnvironmentDescription> getEnvironmentDescriptionPredicate(WaitForEnvironmentContext context) {
        // as well as those (which are used as predicate variables, thus being
        // final)
        final String environmentRef = context.getEnvironmentRef();
        final String statusToWaitFor = defaultString(context.getStatusToWaitFor(), "!Terminated");
        final String healthToWaitFor = context.getHealth();

        // Sanity Check
        Validate.isTrue(isNotBlank(environmentRef), "EnvironmentRef is blank or null", environmentRef);

        // some argument juggling

        final boolean negated = statusToWaitFor.startsWith("!");

        // argument juggling

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

        if (environmentRef.matches("e-\\p{Alnum}{10}")) {
            envPredicate = Predicates.and(envPredicate, new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    return t.getEnvironmentId().equals(environmentRef);
                }
            });

            info("... with environmentId equal to '%s'", environmentRef);
        } else if (environmentRef.matches(".*\\Q.elasticbeanstalk.com\\E")) {
            envPredicate = Predicates.and(envPredicate, new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    return defaultString(t.getCNAME()).equals(environmentRef);
                }
            });
            info("... with cname set to '%s'", environmentRef);
        } else {
            String tmpRE = Pattern.quote(environmentRef);

            if (environmentRef.endsWith("*"))
                tmpRE = format("^\\Q%s\\E.*", environmentRef.substring(0, -1 + environmentRef.length()));

            final String environmentRefNameRE = tmpRE;

            envPredicate = Predicates.and(envPredicate, new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    return t.getEnvironmentName().matches(environmentRefNameRE);
                }
            });

            info("... with environmentName matching re '%s'", environmentRefNameRE);
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
        return envPredicate;
    }

    public EnvironmentDescription executeInternal(
			WaitForEnvironmentContext context) throws Exception {
		// Those are invariants
		long timeoutMins = context.getTimeoutMins();

		Date expiresAt = new Date(System.currentTimeMillis() + MINS_TO_MSEC
				* timeoutMins);
		Date lastMessageRecord = new Date();

        parentMojo.getLog().info("Environment Lookup");

        Predicate<EnvironmentDescription> envPredicate = getEnvironmentDescriptionPredicate(context);

		do {
            DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().withApplicationName(context.getApplicationName()).withIncludeDeleted(true);

            final List<EnvironmentDescription> envs = parentMojo.getService().describeEnvironments(req).getEnvironments();

            Collection<EnvironmentDescription> validEnvironments = Collections2.filter(envs, envPredicate);

			debug("There are %d environments", validEnvironments.size());

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

	boolean timedOutP(Date expiresAt) throws MojoExecutionException {
		return expiresAt.before(new Date(System.currentTimeMillis()));
	}

	public void sleepInterval(long pollInterval) {
		info("Sleeping for %d seconds", pollInterval / 1000);
		try {
			Thread.sleep(pollInterval);
		} catch (InterruptedException e) {
		}
	}
}

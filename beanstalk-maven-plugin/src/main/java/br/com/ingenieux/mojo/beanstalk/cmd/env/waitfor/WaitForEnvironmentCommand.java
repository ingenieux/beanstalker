package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;
import br.com.ingenieux.mojo.beanstalk.env.AWSIdEnvironmentReference;
import br.com.ingenieux.mojo.beanstalk.env.CNamePrefixEnvironmentReference;
import br.com.ingenieux.mojo.beanstalk.env.EnvironmentReference;
import br.com.ingenieux.mojo.beanstalk.env.NameEnvironmentReference;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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


    /**
     * Poll Interval
     */
    public static final long POLL_INTERVAL = 30 * 1000;
    /**
     * Magic Constant for Mins to MSEC
     */
    private static final long MINS_TO_MSEC = 60 * 1000;

    private Map<Class<? extends EnvironmentReference>, EnvironmentDescriptionPredicateFactory>
            envReferencePredicateFactoryRegistry = new HashMap<Class<? extends EnvironmentReference>, EnvironmentDescriptionPredicateFactory>();

    /**
     * Constructor
     *
     * @param parentMojo parent mojo
     */
    public WaitForEnvironmentCommand(AbstractBeanstalkMojo parentMojo)
            throws MojoExecutionException {
        super(parentMojo);

        envReferencePredicateFactoryRegistry.put(AWSIdEnvironmentReference.class,
                                                 new AWSIdEnvironmentReferenceEnvironmentDescriptionPredicateFactory());

        envReferencePredicateFactoryRegistry.put(CNamePrefixEnvironmentReference.class,
                                                 new CNamePrefixEnvironmentReferenceEnvironmentDescriptionPredicateFactory());

        envReferencePredicateFactoryRegistry.put(NameEnvironmentReference.class,
                                                 new NameEnvironmentReferenceEnvironmentDescriptionPredicateFactory());
    }

    public Collection<EnvironmentDescription> lookupInternal(WaitForEnvironmentContext context) {
        List<Predicate<EnvironmentDescription>> envPredicates = getEnvironmentDescriptionPredicate(context);

        DescribeEnvironmentsRequest req =
                new DescribeEnvironmentsRequest().withApplicationName(context.getApplicationName())
                        .withIncludeDeleted(true);

        final List<EnvironmentDescription> envs =
                parentMojo.getService().describeEnvironments(req).getEnvironments();

        return Collections2.filter(envs, Predicates.and(envPredicates));
    }

    protected List<Predicate<EnvironmentDescription>> getEnvironmentDescriptionPredicate(WaitForEnvironmentContext context) {
        // as well as those (which are used as predicate variables, thus being
        // final)
        final EnvironmentReference environmentRef = context.getEnvironmentRef();
        final String statusToWaitFor = defaultString(context.getStatusToWaitFor(), "!Terminated");
        final String healthToWaitFor = context.getHealth();

        // Sanity Check
        Validate.isTrue(isNotBlank(environmentRef.getReferenceValue()), "EnvironmentRef is blank or null",
                        environmentRef);

        // some argument juggling
        final boolean negated = statusToWaitFor.startsWith("!");

        // argument juggling
        List<Predicate<EnvironmentDescription>> result = new ArrayList<Predicate<EnvironmentDescription>>();
        result.add(envReferencePredicateFactoryRegistry.get(environmentRef.getClass()).newInstance(environmentRef));

        {
            // start building predicates with the status one - "![status]" must
            // be equal to status or not status
            final int offset = negated ? 1 : 0;
            final String vStatusToWaitFor = statusToWaitFor.substring(offset);

            result.add(new Predicate<EnvironmentDescription>() {
                public boolean apply(EnvironmentDescription t) {

                    boolean result = vStatusToWaitFor.equals(t.getStatus());

                    if (negated) {
                        result = !result;
                    }

                    debug("testing status '%s' as equal as '%s' (negated? %s, offset: %d): %s",
                          vStatusToWaitFor, t.getStatus(), negated, offset,
                          result);

                    return result;
                }
            });

            info("... with status %s set to '%s'", (negated ? "*NOT*" : " "), vStatusToWaitFor);
        }

        {
            if (isNotBlank(healthToWaitFor)) {
                result.add(new Predicate<EnvironmentDescription>() {
                    @Override
                    public boolean apply(EnvironmentDescription t) {
                        return t.getHealth().equals(healthToWaitFor);
                    }
                });

                info("... with health equal to '%s'", healthToWaitFor);
            }
        }
        return result;
    }

    public EnvironmentDescription executeInternal(
            WaitForEnvironmentContext context) throws Exception {
        // Those are invariants
        long timeoutMins = context.getTimeoutMins();

        Date expiresAt = new Date(System.currentTimeMillis() + MINS_TO_MSEC
                * timeoutMins);
        Date lastMessageRecord = new Date();

        info("Environment Lookup");

        List<Predicate<EnvironmentDescription>>
                envPredicates =
                getEnvironmentDescriptionPredicate(context);
        Predicate<EnvironmentDescription> corePredicate = envPredicates.get(0);
        Predicate<EnvironmentDescription> fullPredicate = Predicates.and(envPredicates);

        do {
            DescribeEnvironmentsRequest
                    req =
                    new DescribeEnvironmentsRequest().withApplicationName(context.getApplicationName())
                            .withIncludeDeleted(true);

            final List<EnvironmentDescription>
                    envs =
                    parentMojo.getService().describeEnvironments(req).getEnvironments();

            Collection<EnvironmentDescription>
                    validEnvironments =
                    Collections2.filter(envs, fullPredicate);

            debug("There are %d environments", validEnvironments.size());

            if (1 == validEnvironments.size()) {
                EnvironmentDescription foundEnvironment = validEnvironments.iterator()
                        .next();

                debug("Found environment %s", foundEnvironment);

                return foundEnvironment;
            } else {
                debug("Found %d environments. No good. Ignoring.",
                      validEnvironments.size());

                for (EnvironmentDescription d : validEnvironments) {
                    debug(" ... %s", d);
                }

                // ... but have we've got any closer match? If so, dump recent events

                Collection<EnvironmentDescription>
                        foundEnvironments =
                        Collections2.filter(envs, corePredicate);

                if (1 == foundEnvironments.size()) {
                    EnvironmentDescription foundEnvironment = foundEnvironments.iterator().next();

                    DescribeEventsResult events = service
                            .describeEvents(new DescribeEventsRequest()
                                                    .withApplicationName(foundEnvironment.getApplicationName())
                                                    .withStartTime(
                                                            new Date(1000 + lastMessageRecord
                                                                    .getTime()))
                                                    .withEnvironmentId(foundEnvironment.getEnvironmentId())
                                                    .withSeverity("TRACE"));

                    Set<EventDescription> eventList = new TreeSet<EventDescription>(
                            new EventDescriptionComparator());

                    eventList.addAll(events.getEvents());

                    for (EventDescription d : eventList) {
                        info("%s %s %s", d.getSeverity(), d.getEventDate(), d.getMessage());

                        if (d.getSeverity().equals(("ERROR"))) {
                            throw new MojoExecutionException(
                                    "Something went wrong in while waiting for the environment setup to complete : "
                                            + d.getMessage());
                        }
                        lastMessageRecord = d.getEventDate();
                    }
                }
            }

            sleepInterval(POLL_INTERVAL);
        } while (!timedOutP(expiresAt));

        throw new MojoExecutionException("Timed out");
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

    interface EnvironmentDescriptionPredicateFactory {
        Predicate<EnvironmentDescription> newInstance(EnvironmentReference envRef);
    }

    static class EventDescriptionComparator implements
            Comparator<EventDescription> {

        @Override
        public int compare(EventDescription o1, EventDescription o2) {
            return o1.getEventDate().compareTo(o2.getEventDate());
        }
    }

    class AWSIdEnvironmentReferenceEnvironmentDescriptionPredicateFactory implements EnvironmentDescriptionPredicateFactory {

        @Override
        public Predicate<EnvironmentDescription> newInstance(EnvironmentReference envRef) {
            final String envId = envRef.getReferenceValue();
            info("... with environmentId equal to '%s'", envId);

            return new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    return t.getEnvironmentId().equals(envId);
                }
            };
        }
    }

    class CNamePrefixEnvironmentReferenceEnvironmentDescriptionPredicateFactory implements EnvironmentDescriptionPredicateFactory {
        @Override
        public Predicate<EnvironmentDescription> newInstance(EnvironmentReference envRef) {
            final String cnamePrefix = envRef.getReferenceValue();
            info("... with cname prefix set to '%s'", cnamePrefix);
            return new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    String cname = t.getCNAME();
                    String environmentCNamePrefix = StringUtils.substringBefore(cname, ".");
                    return defaultString(environmentCNamePrefix).equals(cnamePrefix);
                }
            };
        }
    }

    class NameEnvironmentReferenceEnvironmentDescriptionPredicateFactory implements EnvironmentDescriptionPredicateFactory {

        @Override
        public Predicate<EnvironmentDescription> newInstance(EnvironmentReference envRef) {
            String tmpRE = Pattern.quote(envRef.getReferenceValue());

            if (envRef.getReferenceValue().endsWith("*")) {
                tmpRE = format("^\\Q%s\\E.*",
                               envRef.getReferenceValue().substring(0, -1 + envRef.getReferenceValue().length()));
            }

            final String environmentRefNameRE = tmpRE;

            Predicate<EnvironmentDescription> environmentDescriptionPredicate = new Predicate<EnvironmentDescription>() {
                @Override
                public boolean apply(EnvironmentDescription t) {
                    return t.getEnvironmentName().matches(environmentRefNameRE);
                }
            };

            info("... with environmentName matching re '%s'", environmentRefNameRE);
            return environmentDescriptionPredicate;
        }
    }
}

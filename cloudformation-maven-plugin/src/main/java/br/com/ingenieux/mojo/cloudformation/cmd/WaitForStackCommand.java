package br.com.ingenieux.mojo.cloudformation.cmd;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class WaitForStackCommand {
    private final WaitForStackContext ctx;

    public static class WaitForStackContext {
        String stackId;

        AmazonCloudFormationClient client;

        StatusNotifier notifier;

        Integer timeoutMins;

        Set<StackStatus> statusesToMatch;

        public WaitForStackContext(String stackId, AmazonCloudFormationClient client, StatusNotifier notifier, Integer timeoutMins, Collection<StackStatus> statusesToMatch) {
            this.stackId = stackId;
            this.client = client;
            this.notifier = notifier;
            this.timeoutMins = timeoutMins;

            this.statusesToMatch = new HashSet<>();

            this.statusesToMatch.addAll(statusesToMatch);
        }

        public String getStackId() {
            return stackId;
        }

        public AmazonCloudFormationClient getClient() {
            return client;
        }

        public StatusNotifier getNotifier() {
            return notifier;
        }

        public Integer getTimeoutMins() {
            return timeoutMins;
        }

        public Set<StackStatus> getStatusesToMatch() {
            return statusesToMatch;
        }
    }

    public WaitForStackCommand(WaitForStackContext ctx) {
        this.ctx = ctx;
    }

    public void execute() throws Exception {
        Set<StackEvent> events = new TreeSet<>((o1, o2) -> {
            return o1.getEventId().compareTo(o2.getEventId());
        });

        boolean done = false;

        Date timeoutsAt = new Date(System.currentTimeMillis() + 60000L * ctx.getTimeoutMins());

        do {
            boolean timedOut = !timeoutsAt.after(new Date(System.currentTimeMillis()));

            if (timedOut)
                throw new IllegalStateException("Timed Out");

            {
                final DescribeStackEventsRequest req = new DescribeStackEventsRequest().withStackName(ctx.getStackId());
                String nextToken = null;

                do {
                    req.withNextToken(nextToken);

                    Optional<DescribeStackEventsResult> stackEvents = getDescribeStackEventsResult(req);

                    if (!stackEvents.isPresent()) {
                        return;
                    } else {
                        for (StackEvent e : stackEvents.get().getStackEvents()) {
                            if (!events.contains(e)) {
                                ctx.getNotifier().info("" + e);

                                events.add(e);
                            }
                        }
                    }


                } while (null != nextToken);
            }

            {
                final DescribeStacksResult stacks = ctx.getClient().describeStacks(new DescribeStacksRequest().withStackName(ctx.getStackId()));

                Optional<Stack> foundStack = stacks.getStacks()
                        .stream().filter(stack -> ctx.getStatusesToMatch().contains(StackStatus.fromValue(stack.getStackStatus())))
                        .findFirst();

                done = foundStack.isPresent();
            }

            if (!done) {
                Thread.sleep(15000);
            }

        } while (!done);
    }

    private Optional<DescribeStackEventsResult> getDescribeStackEventsResult(DescribeStackEventsRequest req) {
        try {
            return Optional.of(ctx.getClient().describeStackEvents(req));
        } catch (AmazonServiceException e) {
            if (e.getErrorMessage().contains("does not exist")) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }
}

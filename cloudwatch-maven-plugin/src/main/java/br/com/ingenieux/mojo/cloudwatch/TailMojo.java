/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package br.com.ingenieux.mojo.cloudwatch;

import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsResult;
import com.amazonaws.services.logs.model.FilterLogEventsRequest;
import com.amazonaws.services.logs.model.FilterLogEventsResult;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.OrderBy;
import com.amazonaws.services.logs.model.OutputLogEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.awt.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import br.com.ingenieux.mojo.aws.util.GlobUtil;

import static java.util.Arrays.asList;

/**
 * Represents Tailing a set of CloudWatch Log Streams Together
 */
@Mojo(name = "tail")
public class TailMojo extends AbstractCloudWatchMojo {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.basicDateTime();

    /**
     * Set of Log Groups to Use
     */
    @Parameter(property = "cloudwatch.logGroups", defaultValue = "*")
    Set<String> logGroups = new LinkedHashSet<>(Collections.singletonList("*"));

    protected Set<Pattern> logGroupPatterns;

    public void setLogGroups(String logGroups) {
        this.logGroups = new LinkedHashSet<>(asList(logGroups.split(",")));
    }

    @Override
    protected Object executeInternal() throws Exception {
        this.logGroupPatterns = createLogGroupPatterns();

        AtomicLong baseOffset = new AtomicLong(System.currentTimeMillis() - 60 * 1000);

        Set<String> foundLogGroups = findLogGroups();

        getLog().info("Log Groups Found:");

        foundLogGroups.forEach(logGroupName -> getLog().info(" * " + logGroupName));

        while (true) {
            OptionalLong nextMessageOffset = foundLogGroups
                    .parallelStream()
                    .mapToLong(logGroup -> doLogMessages(logGroup, baseOffset.get()))
                    .max();

            if (nextMessageOffset.isPresent()) {
                baseOffset.set(Math.max(baseOffset.get(), nextMessageOffset.getAsLong()));
            }

            Thread.sleep(5000L);
        }

        //return null;
    }

    private Long doLogMessages(String logGroup, Long baseOffset) {
        String nextToken = null;

        FilterLogEventsRequest req = new FilterLogEventsRequest()
                .withStartTime(baseOffset)
                .withLogGroupName(logGroup)
                ;

        AtomicLong result = new AtomicLong(-1L);

        do {
            final FilterLogEventsResult response = getService()
                    .filterLogEvents(
                            req.withNextToken(nextToken)
                    );

            response.getEvents().forEach(logEvent -> {
                result.set(Math.max(logEvent.getTimestamp(), result.get()));

                String message = String.format(
                        "%s %s %s %s\n%s",
                        DATE_TIME_FORMATTER.print(logEvent.getTimestamp()),
                        StringUtils.abbreviateMiddle(logGroup, "..", 40),
                        StringUtils.abbreviateMiddle(logEvent.getLogStreamName(), "..", 40),
                        StringUtils.abbreviateMiddle(logEvent.getEventId(), "..", 40),
                        logEvent.getMessage().trim()
                );

                getLog().info(message);
            });

            nextToken = response.getNextToken();
        } while (null != nextToken);

        return result.get();
    }

    private Set<String> findLogGroups() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String nextToken = null;
        DescribeLogGroupsRequest req = new DescribeLogGroupsRequest();

        do {
            req.setNextToken(nextToken);

            DescribeLogGroupsResult response = getService().describeLogGroups(req);

            result.addAll(
                    response
                            .getLogGroups()
                            .stream()
                            .filter(
                                    x -> hasMatchingName(x.getLogGroupName(), logGroupPatterns)
                            )
                            .map(x -> x.getLogGroupName())
                            .collect(Collectors.toList())
            );

            nextToken = response.getNextToken();
        } while (null != nextToken);

        return result;
    }

    protected boolean hasMatchingName(String logGroupName, Set<Pattern> logGroupPatterns) {
        final Optional<Pattern> foundFirst = logGroupPatterns.stream()
                .filter(x -> x.matcher(logGroupName).matches())
                .findFirst();

        return foundFirst.isPresent();
    }

    private Set<Pattern> createLogGroupPatterns() {
        return logGroups.stream()
                .map(x -> GlobUtil.asPattern(x))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

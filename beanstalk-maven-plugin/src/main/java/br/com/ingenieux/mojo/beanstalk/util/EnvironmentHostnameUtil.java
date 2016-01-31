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
 */

package br.com.ingenieux.mojo.beanstalk.util;

import com.google.common.base.Predicate;

import com.amazonaws.regions.Region;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.commons.lang.StringUtils;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class EnvironmentHostnameUtil {
    public static final Pattern PATTERN_HOSTNAME = Pattern.compile("(?<cnamePrefix>[\\p{Alnum}\\-]{4,63})(?<regionName>.\\p{Alpha}{2}\\-\\p{Alpha}{4,9}\\-\\p{Digit})?\\Q.elasticbeanstalk.com\\E$");

    public static Predicate<EnvironmentDescription> getHostnamePredicate(Region region, String cnamePrefix) {
        final Set<String> hostnamesToMatch = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        hostnamesToMatch.add(format("%s.elasticbeanstalk.com", cnamePrefix).toLowerCase());
        hostnamesToMatch.add(format("%s.%s.elasticbeanstalk.com", cnamePrefix, region.getName()).toLowerCase());

        return new Predicate<EnvironmentDescription>() {
            @Override
            public boolean apply(EnvironmentDescription t) {
                return hostnamesToMatch.contains(t.getCNAME());
            }

            @Override
            public String toString() {
                return format("... with cname belonging to %s", StringUtils.join(hostnamesToMatch.iterator(), " or "));
            }
        };
    }

    public static String ensureSuffix(String cname, Region region) {
        if (PATTERN_HOSTNAME.matcher(cname).matches()) {
            return cname;
        } else {
            return format("%s.%s.elasticbeanstalk.com", cname, region.toString());
        }
    }

    public static String ensureSuffixStripped(String cnamePrefix) {
        final Matcher matcher = PATTERN_HOSTNAME.matcher(cnamePrefix);

        if (matcher.matches()) {
            return matcher.group("cnamePrefix");
        }

        return cnamePrefix;
    }
}

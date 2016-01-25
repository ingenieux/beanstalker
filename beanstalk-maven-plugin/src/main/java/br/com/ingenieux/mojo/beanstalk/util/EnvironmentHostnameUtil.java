package br.com.ingenieux.mojo.beanstalk.util;

import com.amazonaws.regions.Region;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class EnvironmentHostnameUtil {
    public static final Pattern PATTERN_HOSTNAME = Pattern.compile("(?<cnamePrefix>[\\p{Alnum}\\-]{4,63})(?<regionName>.\\p{Alpha}{2}\\-\\p{Alpha}{4,9}\\-\\p{Digit})?\\Q.elasticbeanstalk.com\\E$");

    public static Predicate<EnvironmentDescription> getHostnamePredicate(Region region, String cnamePrefix) {
        final Set<String> hostnamesToMatch = new TreeSet<String>();

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
}

package br.com.ingenieux.mojo.aws.util;

import java.util.regex.Pattern;

public class GlobUtil {
    public static Pattern globify(String templateName) {
        return Pattern.compile(templateName.replaceAll("\\.", "\\\\.").replaceAll("\\Q*\\E", ".*")
                .replaceAll("\\Q?\\E", "."));
    }

    public static boolean hasWildcards(String input) {
        return (input.indexOf('*') != -1 || input.indexOf('?') != -1);
    }

}

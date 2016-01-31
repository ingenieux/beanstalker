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

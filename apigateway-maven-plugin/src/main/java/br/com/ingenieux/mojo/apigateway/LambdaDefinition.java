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

package br.com.ingenieux.mojo.apigateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang.builder.CompareToBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LambdaDefinition {
    String arn;

    Api api;

    public String getArn() {
        return arn;
    }

    public Api getApi() {
        return api;
    }

    public static class Api implements Comparable<Api> {
        String path;

        String methodType;

        String template;

        boolean corsEnabled;

        public String getPath() {
            return path;
        }

        public String getMethodType() {
            return methodType;
        }

        public String getTemplate() {
            return template;
        }

        public boolean isCorsEnabled() {
            return corsEnabled;
        }

        @Override
        public int compareTo(Api o) {
            if (null == o)
                return -1;

            if (this == o)
                return 0;

            return new CompareToBuilder()
                    .append(this.path, o.path)
                    .append(this.methodType, o.methodType)
                    .toComparison();
        }
    }
}

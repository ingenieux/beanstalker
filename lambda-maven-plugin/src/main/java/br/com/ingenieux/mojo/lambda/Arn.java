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

package br.com.ingenieux.mojo.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arn {
    private static final List<Pattern> PATTERN_ARNS = Arrays.asList(
            Pattern.compile("^arn:" +
                    "(?<partition>[\\w\\-]):" +
                    "(?<service>[\\w\\-]):" +
                    "(?<region>[\\w\\-]):" +
                    "(?<accountId>[\\w\\-]):" +
                    "(?<resource>[\\w\\-])"),

            Pattern.compile("^arn:" +
                    "(?<partition>[\\w\\-]):" +
                    "(?<service>[\\w\\-]):" +
                    "(?<region>[\\w\\-]):" +
                    "(?<accountId>[\\w\\-]):" +
                    "(?<resourceType>[\\w\\-]):" +
                    "(?<resource>[\\w\\-])"),

            Pattern.compile("^arn:" +
                    "(?<partition>[\\w\\-]):" +
                    "(?<service>[\\w\\-]):" +
                    "(?<region>[\\w\\-]):" +
                    "(?<accountId>[\\w\\-]):" +
                    "(?<resourceType>[\\w\\-])/" +
                    "(?<resource>[\\w\\-])")
    );

    String sourceArn;

    String partition;

    String service;

    String region;

    String accountId;

    String resource;

    String resourceType;

    public Arn(String sourceArn, String partition, String service, String region, String accountId, String resource, String resourceType) {
        this.sourceArn = sourceArn;
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resource = resource;
        this.resourceType = resourceType;
    }

    public String getSourceArn() {
        return sourceArn;
    }

    public void setSourceArn(String sourceArn) {
        this.sourceArn = sourceArn;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static Arn lookupArn(String str) {
        for (Pattern p : PATTERN_ARNS) {
            Matcher m = p.matcher(str);

            if (m.matches()) {
                String partition = m.group("partition");
                String service = m.group("service");
                String region = m.group("region");
                String accountId = m.group("accountId");
                String resource = m.group("resource");
                String resourceType = m.group("resourceType");

                return new Arn(str, partition, service, region, accountId, resource, resourceType);
            }
        }

        return null;
    }
}

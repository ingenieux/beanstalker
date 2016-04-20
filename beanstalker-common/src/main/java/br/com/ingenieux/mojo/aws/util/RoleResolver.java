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

package br.com.ingenieux.mojo.aws.util;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by aldrin on 08/04/16.
 */
public class RoleResolver {
    public static final Pattern PATTERN_IAM_ROLE = Pattern.compile("arn:aws:iam:[\\w\\-]*:(\\d+):(.*)");

    private final AmazonIdentityManagement iam;

    private final String accountId;

    Set<String> roles = new LinkedHashSet<String>();

    public RoleResolver(AmazonIdentityManagement iam) {
        this.iam = iam;
        this.roles = loadRoles();

        final String firstRole = roles.iterator().next();

        final Matcher m = PATTERN_IAM_ROLE.matcher(firstRole);

        if (! m.find())
            throw new IllegalStateException("Unable to find account id!");

        this.accountId = m.group(1);
    }

    public String getAccountId() {
        return accountId;
    }

    private Set<String> loadRoles() {
        Set<String> result = new TreeSet<String>();

        boolean done = false;
        String marker = null;
        do {
            final ListRolesRequest listRolesRequest = new ListRolesRequest();

            listRolesRequest.setMarker(marker);

            final ListRolesResult listRolesResult = iam.listRoles(listRolesRequest);

            for (Role r : listRolesResult.getRoles()) {
                result.add(r.getArn());
            }

            done = (!listRolesResult.isTruncated());

            marker = listRolesResult.getMarker();
        } while (!done);

        return result;
    }

    public String lookupRoleGlob(String role) {
        if (GlobUtil.hasWildcards(role)) {
            //getLog().info(format("Looking up IAM Role '%s'", role));

            Pattern p = GlobUtil.globify(role);

            for (String s : roles) {
                if (p.matcher(s).matches()) {
                    //getLog().info(format("Found Role: '%s'", s));

                    return s;
                }
            }

            throw new IllegalStateException("Unable to lookup role '" + role + "': Not found");
        } else {
            //getLog().info(format("Using Role as is: '%s'", role));

            return role;
        }
    }
}

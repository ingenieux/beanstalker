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

package br.com.ingenieux.mojo.cloudformation;

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

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;
import br.com.ingenieux.mojo.aws.util.GlobUtil;
import br.com.ingenieux.mojo.cloudformation.cmd.StatusNotifier;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * CloudFormation Mojo Parent
 */
public abstract class AbstractCloudformationMojo extends AbstractAWSMojo<AmazonCloudFormationClient> implements StatusNotifier {
  @Parameter(property = "project", required = true)
  protected MavenProject curProject;

  @Parameter(property = "cloudformation.stackId")
  String stackId;

  @Parameter(property = "cloudformation.stackName", defaultValue = "${project.artifactId}")
  String stackName;

  @Parameter(property = "cloudformation.roleArn")
  String roleArn;
  
  /**
   * Matched Stack Summary (when found)
   */
  StackSummary stackSummary;

  /**
   * Looks up a stack, throwing an exception if not found and failIfMissing set to true
   *
   * @param failIfMissing whether or not to throw an exception
   * @return true if execution must abort. False otherwise.
   */
  protected boolean shouldFailIfMissingStack(boolean failIfMissing) {
    try {
      ensureStackLookup();

      return false;
    } catch (IllegalStateException e) {
      if (failIfMissing) {
        throw e;
      } else {
        getLog().warn("Stack not found, but failIfMissing set to false (its default). Skipping.");

        return false;
      }
    }
  }

  /**
   * Lookups a Stack
   */
  protected void ensureStackLookup() {
    if (isNotEmpty(stackId)) return;

    getLog().info("Looking up stackId (stack name: " + stackName + ")");

    final Pattern namePattern;

    if (GlobUtil.hasWildcards(stackName)) {
      namePattern = GlobUtil.globify(stackName);
    } else {
      namePattern = Pattern.compile(Pattern.quote(stackName));
    }

    String nextToken = null;
    final ListStacksRequest req =
        new ListStacksRequest().withStackStatusFilters(
          StackStatus.CREATE_COMPLETE,
          StackStatus.CREATE_FAILED,
          StackStatus.UPDATE_COMPLETE,
          StackStatus.ROLLBACK_COMPLETE,
          StackStatus.UPDATE_ROLLBACK_COMPLETE,
          StackStatus.UPDATE_ROLLBACK_FAILED
        );

    do {
      req.setNextToken(nextToken);

      final ListStacksResult result = getService().listStacks(req);

      final Optional<StackSummary> matchedStackSummary =
          result.getStackSummaries().stream().filter(x -> namePattern.matcher(x.getStackName()).matches()).findFirst();

      if (matchedStackSummary.isPresent()) {
        getLog().info("Found stack (stackSummary: " + matchedStackSummary.get());

        this.stackId = matchedStackSummary.get().getStackId();
        this.stackSummary = matchedStackSummary.get();

        return;
      }

      nextToken = result.getNextToken();
    } while (null != nextToken);

    throw new IllegalStateException("Stack '" + stackName + "' not found!");
  }

  protected Map.Entry<String, String> extractNVPair(String nvPair) {
    MapEntry<String, String> result = new MapEntry<>();

    int n = nvPair.indexOf('=');

    if (-1 == n) {
      result.setKey(nvPair);
    } else {
      String k = nvPair.substring(0, n);
      String v = nvPair.substring(1 + n);

      result.setKey(k);
      result.setValue(v);
    }

    getLog().info("Adding/Overwriting Parameter: " + result);

    return result;
  }

  public void info(CharSequence msg) {
    getLog().info(msg);
  }

  public static class MapEntry<K, V> implements Map.Entry<K, V> {
    K key;

    V value;

    @Override
    public K getKey() {
      return key;
    }

    public void setKey(K key) {
      this.key = key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      this.value = value;

      return value;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();

      result.append("" + key);

      if (null != value) {
        result.append("=");
        result.append("" + value);
      }

      return result.toString();
    }
  }
}

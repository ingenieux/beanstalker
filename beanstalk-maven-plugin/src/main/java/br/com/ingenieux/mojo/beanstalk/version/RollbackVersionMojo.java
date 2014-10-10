package br.com.ingenieux.mojo.beanstalk.version;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Deletes application versions, either by count and/or by date old
 *
 * @author Aldrin Leal
 * @since 0.2.3
 */
@Mojo(name = "rollback-version")
public class RollbackVersionMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * Simulate deletion changing algorithm?
   */
  @Parameter(property = "beanstalk.dryRun", defaultValue = "true")
  boolean dryRun;

  /**
   * Updates to the latest version instead?
   */
  @Parameter(property = "beanstalk.latestVersionInstead")
  boolean latestVersionInstead;

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    // TODO: Deal with withVersionLabels
    DescribeApplicationVersionsRequest
        describeApplicationVersionsRequest =
        new DescribeApplicationVersionsRequest()
            .withApplicationName(applicationName);

    DescribeApplicationVersionsResult appVersions = getService()
        .describeApplicationVersions(describeApplicationVersionsRequest);

    DescribeEnvironmentsRequest describeEnvironmentsRequest = new DescribeEnvironmentsRequest()
        .withApplicationName(applicationName).withEnvironmentIds(curEnv.getEnvironmentId())
        .withEnvironmentNames(curEnv.getEnvironmentName()).withIncludeDeleted(false);

    DescribeEnvironmentsResult environments = getService()
        .describeEnvironments(describeEnvironmentsRequest);

    List<ApplicationVersionDescription>
        appVersionList =
        new ArrayList<ApplicationVersionDescription>(
            appVersions.getApplicationVersions());

    List<EnvironmentDescription> environmentList = environments
        .getEnvironments();

    if (environmentList.isEmpty()) {
      throw new MojoFailureException("No environments were found");
    }

    EnvironmentDescription d = environmentList.get(0);

    Collections.sort(appVersionList,
                     new Comparator<ApplicationVersionDescription>() {
                       @Override
                       public int compare(ApplicationVersionDescription o1,
                                          ApplicationVersionDescription o2) {
                         return new CompareToBuilder().append(o1.getDateUpdated(),
                                                              o2.getDateUpdated()).toComparison();
                       }
                     });

    Collections.reverse(appVersionList);

    if (latestVersionInstead) {
      ApplicationVersionDescription latestVersionDescription = appVersionList
          .get(0);

      return changeToVersion(d, latestVersionDescription);
    }

    ListIterator<ApplicationVersionDescription> versionIterator = appVersionList
        .listIterator();

    String curVersionLabel = d.getVersionLabel();

    while (versionIterator.hasNext()) {
      ApplicationVersionDescription versionDescription = versionIterator.next();

      String versionLabel = versionDescription.getVersionLabel();

      if (curVersionLabel.equals(versionLabel) && versionIterator.hasNext()) {
        return changeToVersion(d, versionIterator.next());
      }
    }

    throw new MojoFailureException(
        "No previous version was found (current version: " + curVersionLabel);
  }

  Object changeToVersion(EnvironmentDescription d,
                         ApplicationVersionDescription latestVersionDescription) {
    String curVersionLabel = d.getVersionLabel();
    String versionLabel = latestVersionDescription.getVersionLabel();

    UpdateEnvironmentRequest request = new UpdateEnvironmentRequest()
        .withEnvironmentId(d.getEnvironmentId()).withVersionLabel(versionLabel);

    getLog().info(
        "Changing versionLabel for Environment[name=" + curEnv.getEnvironmentName()
        + "; environmentId=" + curEnv.getEnvironmentId() + "] from version "
        + curVersionLabel + " to version " + latestVersionDescription.getVersionLabel());

    if (dryRun) {
      return null;
    }

    return getService().updateEnvironment(request);
  }
}

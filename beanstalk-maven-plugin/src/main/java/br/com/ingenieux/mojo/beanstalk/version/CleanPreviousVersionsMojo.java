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
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Deletes application versions, either by count and/or by date old
 *
 * @since 0.2.2
 */
@Mojo(name = "clean-previous-versions")
public class CleanPreviousVersionsMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  /**
   * Delete the source bundle?
   */
  @Parameter(property = "beanstalk.deleteSourceBundle", defaultValue = "false")
  boolean deleteSourceBundle;

  /**
   * How many versions to keep?
   */
  @Parameter(property = "beanstalk.versionsToKeep")
  Integer versionsToKeep;

  /**
   * How many versions to keep?
   */
  @Parameter(property = "beanstalk.daysToKeep")
  Integer daysToKeep;

  /**
   * Filter application version list to examine for cleaning based on java.util.regex.Pattern
   * string.
   */
  @Parameter(property = "beanstalk.cleanFilter")
  String cleanFilter;

  /**
   * Simulate deletion changing algorithm?
   */
  @Parameter(property = "beanstalk.dryRun", defaultValue = "true")
  boolean dryRun;

  private int deletedVersionsCount;

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    boolean bVersionsToKeepDefined = (null != versionsToKeep);
    boolean bDaysToKeepDefined = (null != daysToKeep);

    if (!(bVersionsToKeepDefined ^ bDaysToKeepDefined)) {
      throw new MojoFailureException(
          "Declare either versionsToKeep or daysToKeep, but not both nor none!");
    }

    DescribeApplicationVersionsRequest
        describeApplicationVersionsRequest =
        new DescribeApplicationVersionsRequest()
            .withApplicationName(applicationName);

    DescribeApplicationVersionsResult appVersions = getService()
        .describeApplicationVersions(describeApplicationVersionsRequest);

    DescribeEnvironmentsResult environments = getService()
        .describeEnvironments();

    List<ApplicationVersionDescription>
        appVersionList =
        new ArrayList<ApplicationVersionDescription>(
            appVersions.getApplicationVersions());

    deletedVersionsCount = 0;

    for (EnvironmentDescription d : environments.getEnvironments()) {
      boolean bActiveEnvironment = (d.getStatus().equals("Running")
                                    || d.getStatus().equals("Launching") || d.getStatus()
          .equals("Ready"));

      for (ListIterator<ApplicationVersionDescription> appVersionIterator = appVersionList
          .listIterator(); appVersionIterator.hasNext(); ) {
        ApplicationVersionDescription appVersion = appVersionIterator
            .next();

        boolean bMatchesVersion = appVersion.getVersionLabel().equals(
            d.getVersionLabel());

        if (bActiveEnvironment && bMatchesVersion) {
          getLog().info(
              "VersionLabel " + appVersion.getVersionLabel()
              + " is bound to environment "
              + d.getEnvironmentName() + " - Skipping it");

          appVersionIterator.remove();
        }
      }
    }

    filterAppVersionListByVersionLabelPattern(appVersionList, cleanFilter);

    Collections.sort(appVersionList,
                     new Comparator<ApplicationVersionDescription>() {
                       @Override
                       public int compare(ApplicationVersionDescription o1,
                                          ApplicationVersionDescription o2) {
                         return new CompareToBuilder().append(
                             o1.getDateUpdated(), o2.getDateUpdated())
                             .toComparison();
                       }
                     });

    if (bDaysToKeepDefined) {
      Date now = new Date();

      for (ApplicationVersionDescription d : appVersionList) {
        long delta = now.getTime() - d.getDateUpdated().getTime();

        delta /= 1000;
        delta /= 86400;

        boolean shouldDeleteP = (delta > daysToKeep);

        if (getLog().isDebugEnabled()) {
          getLog().debug(
              "Version " + d.getVersionLabel() + " was from "
              + delta + " days ago. Should we delete? "
              + shouldDeleteP);
        }

        if (shouldDeleteP) {
          deleteVersion(d);
        }
      }
    } else {
      while (appVersionList.size() > versionsToKeep) {
        deleteVersion(appVersionList.remove(0));
      }
    }

    getLog().info(
        "Deleted " + deletedVersionsCount + " versions.");

    return null;
  }

  void deleteVersion(ApplicationVersionDescription versionToRemove) {
    getLog().info(
        "Must delete version: " + versionToRemove.getVersionLabel());

    DeleteApplicationVersionRequest req = new DeleteApplicationVersionRequest()
        .withApplicationName(versionToRemove.getApplicationName())//
        .withDeleteSourceBundle(deleteSourceBundle)//
        .withVersionLabel(versionToRemove.getVersionLabel());

    if (!dryRun) {
      getService().deleteApplicationVersion(req);
      deletedVersionsCount++;
    }
  }

  void filterAppVersionListByVersionLabelPattern(List<ApplicationVersionDescription> appVersionList,
                                                 String patternString) {
    if (patternString == null) {
      return;
    }

    getLog().info(
        "Filtering versions with pattern : " + patternString);

    Pattern p = Pattern.compile(patternString);
    for (ListIterator<ApplicationVersionDescription> appVersionIterator = appVersionList
        .listIterator(); appVersionIterator.hasNext(); ) {

      if (!p.matcher(appVersionIterator
                         .next()
                         .getVersionLabel())
          .matches()) {
        appVersionIterator.remove();
      }
    }
  }
}

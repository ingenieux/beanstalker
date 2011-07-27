package br.com.ingenieux.mojo.beanstalk;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

/**
 * Deletes application versions, either by count and/or by date old
 * 
 * @goal rollback-version
 * 
 * @author Aldrin Leal
 */
public class RollbackVersionMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${beanstalk.applicationName}"
	 *            default-value="${project.artifactId}"
	 * @required
	 */
	String applicationName;

	/**
	 * Simulate deletion changing algorithm?
	 * 
	 * @parameter expression="${beanstalk.dryRun}" default-value=true
	 */
	boolean dryRun;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentName}"
	 *            default-value="default"
	 */
	String environmentName;

	/**
	 * Environment Id
	 * 
	 * @parameter expression="${beanstalk.environmentId}"
	 */
	String environmentId;

	/**
	 * Updates to the latest version instead?
	 * 
	 * @parameter expression="${beanstalk.latestVersionInstead}"
	 *            default-value=false
	 */
	boolean latestVersionInstead;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		// TODO: Deal with withVersionLabels
		DescribeApplicationVersionsRequest describeApplicationVersionsRequest = new DescribeApplicationVersionsRequest()
		    .withApplicationName(applicationName);

		DescribeApplicationVersionsResult appVersions = service
		    .describeApplicationVersions(describeApplicationVersionsRequest);

		DescribeEnvironmentsRequest describeEnvironmentsRequest = new DescribeEnvironmentsRequest()
		    .withApplicationName(applicationName).withEnvironmentIds(environmentId)
		    .withEnvironmentNames(environmentName).withIncludeDeleted(false);

		DescribeEnvironmentsResult environments = service
		    .describeEnvironments(describeEnvironmentsRequest);

		List<ApplicationVersionDescription> appVersionList = new ArrayList<ApplicationVersionDescription>(
		    appVersions.getApplicationVersions());

		List<EnvironmentDescription> environmentList = environments
		    .getEnvironments();

		if (environmentList.isEmpty())
			throw new MojoFailureException("No environments were found");

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
		    "Changing versionLabel for Environment[name=" + environmentName
		        + "; environmentId=" + environmentId + "] from version "
		        + curVersionLabel + " to version " + latestVersionDescription.getVersionLabel());

		if (dryRun)
			return null;

		return service.updateEnvironment(request);
	}
}

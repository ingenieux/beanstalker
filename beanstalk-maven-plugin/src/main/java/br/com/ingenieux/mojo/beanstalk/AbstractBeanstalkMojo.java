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


import static org.apache.commons.lang.StringUtils.isBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public abstract class AbstractBeanstalkMojo extends
		AbstractAWSMojo<AWSElasticBeanstalkClient> {
	protected List<ConfigurationOptionSetting> getOptionSettings(
			ConfigurationOptionSetting[] optionSettings) {
		ConfigurationOptionSetting[] arrOptionSettings = optionSettings;

		if (null == arrOptionSettings || 0 == arrOptionSettings.length)
			return Collections.emptyList();

		return Arrays.asList(arrOptionSettings);
	}

	protected EnvironmentDescription lookupEnvironment(String applicationName, String environmentCNamePrefix) throws MojoExecutionException {
		if (isBlank(environmentCNamePrefix))
			throw new MojoExecutionException("You must declare cnamePrefix");

		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
				.withApplicationName(applicationName);
		DescribeEnvironmentsResult result = null;

		String cNameToFind = String.format("%s.elasticbeanstalk.com",
				environmentCNamePrefix);

		getLog().info("Looking up for " + cNameToFind);

		result = getService().describeEnvironments(req);

		List<EnvironmentDescription> environments = new ArrayList<EnvironmentDescription>();

		for (EnvironmentDescription d : result.getEnvironments())
			if (cNameToFind.equals(d.getCNAME())&& (!d.getStatus().startsWith("Termin")))
				environments.add(d);

		return handleResults(environments);
	}

	protected EnvironmentDescription handleResults(List<EnvironmentDescription> environments)
			throws MojoExecutionException {
		int len = environments.size();

		if (1 == len)
			return environments.get(0);

		handleNonSingle(len);

		return null;
	}

	protected void handleNonSingle(int len)
			throws MojoExecutionException {
		if (0 == len) {
			throw new MojoExecutionException("No environments found");
		} else {
			throw new MojoExecutionException("Multiple environments found matching the supplied parameters (may you file a bug report?)");
		}
	}
}

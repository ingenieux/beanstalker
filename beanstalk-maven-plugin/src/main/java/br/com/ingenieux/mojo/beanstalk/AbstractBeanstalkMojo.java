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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

	@Override
	protected String getEndpoint() {
		if (StringUtils.isNotBlank(region))
			return String.format("elasticbeanstalk.%s.amazonaws.com", region);

		return null;
	}

	protected EnvironmentDescription lookupEnvironment(String applicationName,
			String kind, String environmentId, String environmentName,
			String environmentCNamePrefix) throws MojoExecutionException {
		boolean bIdDefined = isNotBlank(environmentId);
		boolean bNameDefined = isNotBlank(environmentName);
		boolean bCNamePrefixDefined = isNotBlank(environmentCNamePrefix);

		boolean bIdOrNameDefined = bIdDefined ^ bNameDefined;

		if (!(bIdOrNameDefined ^ bCNamePrefixDefined)) {
			String message = "You must declare either _EnvironmentId or _EnvironmentName or _EnvironmentCNamePrefix"
					.replaceAll("_", kind);
			throw new MojoExecutionException(message);
		}

		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
				.withApplicationName(applicationName);
		DescribeEnvironmentsResult result = null;

		if (bIdOrNameDefined) {
			if (bIdDefined) {
				req.setEnvironmentIds(Arrays.asList(environmentId));
			} else if (bNameDefined) {
				req.setEnvironmentNames(Arrays.asList(environmentName));
			}

			result = getService().describeEnvironments(req);

			List<EnvironmentDescription> environments = result
					.getEnvironments();

			return handleResults(kind, environments);
		}

		String cNameToFind = String.format("%s.elasticbeanstalk.com",
				environmentCNamePrefix);

		getLog().info("Looking up for " + cNameToFind);

		result = getService().describeEnvironments(req);

		List<EnvironmentDescription> environments = new ArrayList<EnvironmentDescription>();

		for (EnvironmentDescription d : result.getEnvironments())
			if (cNameToFind.equals(d.getCNAME()))
				environments.add(d);

		return handleResults(kind, environments);
	}

	private EnvironmentDescription handleResults(String kind,
			List<EnvironmentDescription> environments)
			throws MojoExecutionException {
		int len = environments.size();

		if (1 == len)
			return environments.get(0);

		handleNonSingle(kind, len);

		return null;
	}

	private void handleNonSingle(String kind, int len)
			throws MojoExecutionException {
		if (0 == len) {
			String message = "No _ environments found matching the supplied parameters"
					.replaceAll("_", kind);

			throw new MojoExecutionException(message);
		} else {
			String message = "Multiple _ environments found matching the supplied parameters (may you file a bug report?)"
					.replaceAll("_", kind);

			throw new MojoExecutionException(message);
		}
	}
}

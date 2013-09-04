package br.com.ingenieux.mojo.beanstalk.env;

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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;

/**
 * Imports a Given Environment Properties ilocally, suitable for use with the <a
 * href="http://mojo.codehaus.org/properties-maven-plugin/">Properties Maven
 * Plugin</a>
 * 
 * @since 1.1.0
 */
@Mojo(name = "import-environment-settings")
public class ImportEnvironmentSettingsMojo extends AbstractNeedsEnvironmentMojo {
	protected Object executeInternal() throws AbstractMojoExecutionException {
		DescribeConfigurationSettingsResult configSettings = getService()
				.describeConfigurationSettings(
						new DescribeConfigurationSettingsRequest(
								applicationName).withEnvironmentName(curEnv
								.getEnvironmentName()));

		for (ConfigurationOptionSetting d : configSettings
				.getConfigurationSettings().get(0).getOptionSettings()) {
			String key = String.format("beanstalk.%s.%s", d.getNamespace()
					.replaceAll(":", "."), d.getOptionName());
			String value = d.getValue();
			
			if (StringUtils.isBlank(value)) {
				getLog().debug("Ignoring null/blank property for " + key);
				continue;
			}

			for (Map.Entry<String, ConfigurationOptionSetting> cosEntry : COMMON_PARAMETERS.entrySet()) {
				ConfigurationOptionSetting v = cosEntry.getValue();
				
				boolean match = v.getNamespace().equals(d.getNamespace()) && v.getOptionName().equals(d.getOptionName());
				
				if (match) {
					key = cosEntry.getKey();
				}
			}

			getLog().info(String.format("Importing: %s=%s", key, value));

			context.put(key, value);
		}

		return null;
	}
}

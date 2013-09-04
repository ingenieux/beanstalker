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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;

/**
 * Dumps the current mojo context for aws-related variables into the screen or
 * an output file
 * 
 * @see ImportEnvironmentSettingsMojo
 * @since 1.1.0
 */
@Mojo(name = "dump-environment-settings")
public class DumpEnvironmentSettings extends AbstractNeedsEnvironmentMojo {
	/**
	 * (Optional) output file to output to
	 */
	@Parameter(property = "beanstalk.outputFile")
	private File outputFile;
	
	private Map<String, ConfigurationOptionDescription> defaultSettings = new TreeMap<String, ConfigurationOptionDescription>();
	
	protected Object executeInternal() throws Exception {
		DescribeConfigurationOptionsResult configOptions = getService().describeConfigurationOptions(new DescribeConfigurationOptionsRequest().withApplicationName(applicationName).withEnvironmentName(curEnv.getEnvironmentName()));
		
		for (ConfigurationOptionDescription o : configOptions.getOptions()) {
			String key = String.format("beanstalk.env.%s.%s", o.getNamespace().replace(":", "."), o.getName());
			
			for (Map.Entry<String, ConfigurationOptionSetting> entry : COMMON_PARAMETERS.entrySet()) {
				ConfigurationOptionSetting cos = entry.getValue();
				
				if (cos.getNamespace().equals(o.getNamespace()) && cos.getOptionName().equals(o.getName())) {
					key = entry.getKey();
					break;
				}
			}
			
			defaultSettings.put(key, o);
		}
		
		
		DescribeConfigurationSettingsResult configurationSettings = getService()
				.describeConfigurationSettings(
						new DescribeConfigurationSettingsRequest()
								.withApplicationName(applicationName)
								.withEnvironmentName(curEnv.getEnvironmentName()));

		Properties newProperties = new Properties();
		
		if (configurationSettings.getConfigurationSettings().isEmpty()) {
			throw new IllegalStateException("No Configuration Settings received");
		}

		ConfigurationSettingsDescription configSettings = configurationSettings.getConfigurationSettings().get(0);
		
		for (ConfigurationOptionSetting d : configSettings.getOptionSettings()) {
			String key = String.format("beanstalk.env.%s.%s", d.getNamespace().replaceAll(":", "."), d.getOptionName());
			String defaultValue = "";
			String outputKey = key;
			
			for (Map.Entry<String, ConfigurationOptionSetting> cosEntry : COMMON_PARAMETERS.entrySet()) {
				ConfigurationOptionSetting v = cosEntry.getValue();
				
				boolean match = v.getNamespace().equals(d.getNamespace()) && v.getOptionName().equals(d.getOptionName());
				
				if (match) {
					outputKey = cosEntry.getKey();
					break;
				}
			}
			
			if (defaultSettings.containsKey(outputKey))
				defaultValue = StringUtils.defaultString(defaultSettings.get(outputKey).getDefaultValue());
			
			String value = d.getValue();
			
			if (null == value || StringUtils.isBlank("" + value))
				continue;

			if (!defaultValue.equals(value)) {
				getLog().debug("Adding property " + key);
				newProperties.put(outputKey, value);
			} else {
				getLog().debug("Ignoring property " + key + " (defaulted)");
			}
		}

		String comment = "elastic beanstalk environment properties for "
				+ environmentName;
		if (null != outputFile) {
			newProperties.store(new FileOutputStream(outputFile), comment);
		} else {
			newProperties.store(System.out, comment);
		}

		return null;
	}
}

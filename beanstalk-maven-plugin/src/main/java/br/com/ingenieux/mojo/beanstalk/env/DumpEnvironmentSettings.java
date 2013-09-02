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
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationOptionsResult;

/**
 * Dumps the current mojo context for aws-related variables into the screen or an output file
 *
 * @see ImportEnvironmentSettingsMojo
 * @since 1.1.0
 */
@Mojo(name = "dump-environment-settings")
public class DumpEnvironmentSettings extends AbstractNeedsEnvironmentMojo {
	private Context context;

	/**
	 * (Optional) output file to output to
	 */
	@Parameter(property = "beanstalk.outputFile")
	private File outputFile;

	@Override
	public void contextualize(Context context) throws ContextException {
		super.contextualize(context);

		this.context = context;
	}

	protected Object executeInternal() throws Exception {
		DescribeConfigurationOptionsResult configurationOptions = getService()
				.describeConfigurationOptions(
						new DescribeConfigurationOptionsRequest()
								.withApplicationName(applicationName)
								.withEnvironmentName(curEnv.getEnvironmentName()));

		Properties newProperties = new Properties();

		for (ConfigurationOptionDescription d : configurationOptions
				.getOptions()) {
			String key = String.format("%s:%s", d.getNamespace(), d.getName());
			String defaultValue = StringUtils.defaultString(d.getDefaultValue());
			
			if (!context.contains(key))
				continue;

			Object value = context.get(key);
			
			if (null == value || StringUtils.isBlank("" + value))
				continue;

			if (!defaultValue.equals(value)) {
				getLog().debug("Adding property " + key);
				newProperties.put(key, value);
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

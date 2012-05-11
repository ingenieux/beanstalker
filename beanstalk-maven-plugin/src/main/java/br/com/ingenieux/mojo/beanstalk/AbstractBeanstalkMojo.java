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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

public abstract class AbstractBeanstalkMojo extends
		AbstractAWSMojo<AWSElasticBeanstalkClient> {
	
	@MojoParameter(expression="${beanstalk.endpoint}", description="API endpoint")
	protected String endpoint;
	
	protected List<ConfigurationOptionSetting> getOptionSettings(
			ConfigurationOptionSetting[] optionSettings) {
		ConfigurationOptionSetting[] arrOptionSettings = optionSettings;

		if (null == arrOptionSettings || 0 == arrOptionSettings.length)
			return Collections.emptyList();

		return Arrays.asList(arrOptionSettings);
	}
	
	@Override
	protected void configure() {
		if (StringUtils.isEmpty(endpoint) == false) {
			getService().setEndpoint(endpoint);
		}
	}
}

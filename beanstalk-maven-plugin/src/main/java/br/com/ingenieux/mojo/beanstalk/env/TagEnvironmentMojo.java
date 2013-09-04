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

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;

/**
 * Tags an Environment
 * 
 * @since 1.1.0
 */
@Mojo(name = "tag-environment")
public class TagEnvironmentMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * Template Name to use
	 */
	@Parameter(property = "beanstalk.templateName")
	String templateName;

	@Override
	protected Object executeInternal() throws AbstractMojoExecutionException {
		Set<String> configTemplates = new TreeSet<String>(
				super.getConfigurationTemplates(applicationName));

		if (StringUtils.isBlank(templateName)) {
			int i = 1;
			
			do {
				templateName = String.format("%s-%s-%02d", applicationName,
						environmentName, i++);
			} while (configTemplates.contains(templateName));

		}

		return getService().createConfigurationTemplate(
				new CreateConfigurationTemplateRequest().withEnvironmentId(
						curEnv.getEnvironmentId()).withTemplateName(
						templateName));
	}
}

package br.com.ingenieux.mojo.beanstalk.config;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.ConfigurationTemplate;

import com.amazonaws.services.elasticbeanstalk.model.DeleteConfigurationTemplateRequest;

/**
 * Delete Configuration Template
 * 
 * @author Aldrin Leal
 */
@MojoGoal("delete-configuration-templates")
@MojoSince("0.2.7")
public class DeleteConfigurationTemplateMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 */
	@MojoParameter(expression="${beanstalk.applicationName}", defaultValue="${project.artifactId}", required=true, description="Beanstalk Application Name")
	String applicationName;

	/**
	 * Configuration Template Name (Optional)
	 */
	@MojoParameter(expression="${beanstalk.configurationTemplate}")
	String configurationTemplate;
	
	/**
	 * Configuration Templates
	 */
	@MojoParameter
	ConfigurationTemplate[] configurationTemplates;
	
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {

		boolean bConfigurationTemplateDefined = StringUtils
		    .isNotBlank(configurationTemplate);
		
		if (bConfigurationTemplateDefined) {
			deleteConfiguration(configurationTemplate);
		} else {
			for (ConfigurationTemplate template : configurationTemplates)
				deleteConfiguration(template.getId());
		}
		
		return null;
	}

	void deleteConfiguration(String templateName) {
		
		DeleteConfigurationTemplateRequest req = new DeleteConfigurationTemplateRequest(applicationName, templateName);
		
		getService().deleteConfigurationTemplate(req);
  }

}

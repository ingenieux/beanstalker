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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateResult;

/**
 * Describes Available Configuration Templates
 * 
 * @goal describe-configuration-templates
 * 
 * @author Aldrin Leal
 */
public class CreateConfigurationTemplateMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${beanstalk.applicationName}"
	 *            default-value="${project.artifactId}"
	 * @required
	 */
	String applicationName;

	/**
	 * Configuration Template Name (Optional)
	 * 
	 * @parameter expression="${beanstalk.configurationTemplate}"
	 */
	String configurationTemplate;
	
	/**
	 * Configuration Templates
	 * 
	 * @parameter
	 */
	ConfigurationTemplate[] configurationTemplates;
	
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
//		DescribeApplicationsRequest req = new DescribeApplicationsRequest()
//		    .withApplicationNames(applicationName);
		boolean bConfigurationTemplateDefined = StringUtils
		    .isNotBlank(configurationTemplate);
		
		if (bConfigurationTemplateDefined) {
			return createConfiguration(configurationTemplate);
		} else {
			for (ConfigurationTemplate template : configurationTemplates)
				createConfiguration(template.getId());
		}
		
		return null;
	}

	CreateConfigurationTemplateResult createConfiguration(String templateName) {
		ConfigurationTemplate template = getConfigurationTemplate(templateName);
		
		CreateConfigurationTemplateRequest req = new CreateConfigurationTemplateRequest(applicationName, templateName);
		
		req.setSolutionStackName(template.getSolutionStack());
		req.setOptionSettings(Arrays.asList(template.getOptionSettings()));
		
		return service.createConfigurationTemplate(req);
  }

	private ConfigurationTemplate getConfigurationTemplate(String id) {
		for (ConfigurationTemplate template : configurationTemplates)
			if (id.equals(template.getId()))
				return template;
		
	  return null;
  }
}

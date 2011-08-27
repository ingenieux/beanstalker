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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;

/**
 * Describes Available Configuration Templates
 * 
 * @goal describe-configuration-templates
 * 
 * @author Aldrin Leal
 */
public class DescribeConfigurationTemplatesMojo extends AbstractBeanstalkMojo {
	private static final String ENDL = System.getProperty("line.separator");

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
	 * Output file (Optional)
	 * 
	 * @parameter expression="${beanstalk.outputFile}"
	 */
	File outputFile;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DescribeApplicationsRequest req = new DescribeApplicationsRequest()
		    .withApplicationNames(applicationName);
		boolean bConfigurationTemplateDefined = StringUtils
		    .isNotBlank(configurationTemplate);

		DescribeApplicationsResult apps = service.describeApplications(req);

		List<ApplicationDescription> applications = apps.getApplications();

		if (applications.isEmpty()) {
			String errorMessage = "Application ('" + applicationName
			    + "') not found!";

			getLog().warn(errorMessage);

			throw new MojoFailureException(errorMessage);
		}

		ApplicationDescription desc = applications.get(0);

		List<String> configTemplates = desc.getConfigurationTemplates();

		if (bConfigurationTemplateDefined) {
			describeConfigurationTemplate(configurationTemplate);
		} else {
			for (String availConfigTemplate : configTemplates)
				describeConfigurationTemplate(availConfigTemplate);
		}

		return null;
	}

	void describeConfigurationTemplate(String configTemplateName) {
		DescribeConfigurationSettingsRequest req = new DescribeConfigurationSettingsRequest()
		    .withApplicationName(applicationName).withTemplateName(
		        configTemplateName);

		DescribeConfigurationSettingsResult configSettings = service.describeConfigurationSettings(req);
		
		List<String> buf = new ArrayList<String>();
		
		buf.add("<optionSettings>");

		for (ConfigurationSettingsDescription configSetting : configSettings.getConfigurationSettings()) {
			for (ConfigurationOptionSetting setting : configSetting.getOptionSettings()) {
				buf.add("  <optionSetting>");
				buf.add(String.format("    <%s>%s</%1$s>", "namespace", setting.getNamespace()));
				buf.add(String.format("    <%s>%s</%1$s>", "optionName", setting.getOptionName()));
				buf.add(String.format("    <%s>%s</%1$s>", "value", setting.getValue()));
				buf.add("  </optionSetting>");
			}
		}

		buf.add("</optionSettings>");

		if (null != outputFile) {
			getLog().info("Dumping results to file: " + outputFile.getName());
			
			String bufChars = StringUtils.join(buf.iterator(), ENDL);
			FileWriter writer = null;
			
			try {
				writer = new FileWriter(outputFile);

				IOUtils.copy(new StringReader(bufChars), writer);
      } catch (IOException e) {
      	throw new RuntimeException("Failure when writing to file: " + outputFile.getName(), e);
      } finally {
      	IOUtils.closeQuietly(writer);
      }
		} else {
			getLog().info("Dumping results to stdout");
			
			for (String e : buf)
				getLog().info(e);
		}
	}
}

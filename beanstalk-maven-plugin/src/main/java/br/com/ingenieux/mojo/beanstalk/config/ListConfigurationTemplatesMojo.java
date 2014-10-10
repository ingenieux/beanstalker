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

import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import static java.lang.String.format;

/**
 * Describes Available Configuration Templates
 *
 * @author Aldrin Leal
 * @since 1.0-SNAPSHOT
 */
@Mojo(name = "list-configuration-templates", requiresDirectInvocation = true)
public class ListConfigurationTemplatesMojo extends AbstractBeanstalkMojo {

  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  protected String applicationName;

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    DescribeApplicationsRequest req = new DescribeApplicationsRequest()
        .withApplicationNames(applicationName);

    DescribeApplicationsResult apps = getService()
        .describeApplications(req);

    List<ApplicationDescription> applications = apps.getApplications();

    if (applications.isEmpty()) {
      String errorMessage = "Application ('" + applicationName
                            + "') not found!";

      getLog().warn(errorMessage);

      throw new MojoFailureException(errorMessage);
    }

    ApplicationDescription desc = applications.get(0);

    List<String> configTemplates = desc.getConfigurationTemplates();

    getLog().info(format("There are %d config templates", configTemplates.size()));

    return configTemplates;
  }
}

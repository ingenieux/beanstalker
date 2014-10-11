package br.com.ingenieux.mojo.beanstalk.app;

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

import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Creates an Application
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateApplication.html"
 * >CreateApplication API</a> call.
 *
 * @since 0.1.0
 */
@Mojo(name = "create-application")
public class CreateApplicationMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  /**
   * Application Description
   */
  @Parameter(property = "beanstalk.applicationDescription", defaultValue = "${project.name}")
  String applicationDescription;

  protected Object executeInternal() throws MojoExecutionException {
    CreateApplicationRequest request = new CreateApplicationRequest(
        this.applicationName);

    request.setDescription(applicationDescription);

    CreateApplicationResult result = getService().createApplication(request);

    return result;
  }
}

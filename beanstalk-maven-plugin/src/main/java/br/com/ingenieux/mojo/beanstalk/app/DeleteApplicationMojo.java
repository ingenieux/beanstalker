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

import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Deletes an Application
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DeleteApplication.html"
 * >DeleteApplication API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.1.0
 */
@Mojo(name = "delete-application")
public class DeleteApplicationMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    DeleteApplicationRequest req = new DeleteApplicationRequest();

    req.setApplicationName(applicationName);

    getService().deleteApplication(req);

    return null;
  }
}

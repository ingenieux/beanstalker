package br.com.ingenieux.mojo.beanstalk.version;

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

import com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Updates an Application Version
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateApplicationVersion.html"
 * >CreateApplicationVersion API</a> call.
 *
 * @since 0.2.1
 */
@Mojo(name = "update-application-version")
public class UpdateApplicationVersionMojo extends AbstractBeanstalkMojo {

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

  @Parameter(property = "beanstalk.versionLabel", defaultValue = "${project.version}")
  String versionLabel;

  protected Object executeInternal() throws MojoExecutionException {
    UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest();

    request.setApplicationName(applicationName);
    request.setDescription(applicationDescription);
    request.setVersionLabel(versionLabel);

    UpdateApplicationVersionResult result = getService()
        .updateApplicationVersion(request);

    return result.getApplicationVersion();
  }
}

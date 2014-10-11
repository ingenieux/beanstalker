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

import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Describe Existing Application Versions
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeApplicationVersions.html"
 * >DescribeApplicationVersions API</a> call.
 *
 * @since 0.2.4
 */
@Mojo(name = "describe-application-versions")
public class DescribeApplicationVersionsMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  protected String applicationName;

  protected Object executeInternal() throws MojoExecutionException {
    DescribeApplicationVersionsRequest
        describeApplicationVersionsRequest =
        new DescribeApplicationVersionsRequest();

    describeApplicationVersionsRequest.setApplicationName(applicationName);

    return getService()
        .describeApplicationVersions(describeApplicationVersionsRequest);
  }
}

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

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Describe Environment Resources
 *
 * See the docs for <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeEnvironmentResources.html"
 * >DescribeEnvironmentResources API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.2.6
 */
@Mojo(name = "describe-environment-resources")
public class DescribeEnvironmentResourcesMojo extends
                                              AbstractNeedsEnvironmentMojo {

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    return getService()
        .describeEnvironmentResources(new DescribeEnvironmentResourcesRequest()
                                          .withEnvironmentId(curEnv.getEnvironmentId())
                                          .withEnvironmentName(
                                              curEnv.getEnvironmentName()));
  }
}

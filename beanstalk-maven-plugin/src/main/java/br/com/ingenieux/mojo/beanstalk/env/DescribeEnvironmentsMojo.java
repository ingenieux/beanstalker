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

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Describe running environments
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeEnvironments.html"
 * >DescribeEnvironments API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.1.0
 */
@Mojo(name = "describe-environments", requiresDirectInvocation = true)
public class DescribeEnvironmentsMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  protected String applicationName;

  /**
   * Include Deleted?
   */
  @Parameter(property = "beanstalk.includeDeleted")
  boolean includeDeleted;

  /**
   * Output file (Optional)
   */
  @Parameter(property = "beanstalk.outputFile")
  File outputFile;

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest();

    req.setApplicationName(applicationName);
    req.setIncludeDeleted(includeDeleted);

    // TODO add environmentNames / environmentIds / includeDeletedBackTo

    DescribeEnvironmentsResult result = getService().describeEnvironments(req);

    if (null != outputFile) {
      getLog().info("Writing results into " + outputFile.getName());

      try {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();

        writer.writeValue(outputFile, result.getEnvironments());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    }

    return result;
  }
}

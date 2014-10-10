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


import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;

/**
 * Lists the available solution stacks
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_SwapEnvironmentCNAMEs.html"
 * >SwapEnvironmentCNAMEs API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.2.3
 */
@Mojo(name = "swap-environment-cnames")
public class SwapEnvironmentCnamesMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  /**
   * cname of source environment
   */
  @Parameter(property = "beanstalk.sourceEnvironmentCNamePrefix")
  String sourceEnvironmentCNamePrefix;

  /**
   * cname of target environment
   */
  @Parameter(property = "beanstalk.targetEnvironmentCNamePrefix")
  String targetEnvironmentCNamePrefix;

  @Override
  protected Object executeInternal() throws AbstractMojoExecutionException {
    EnvironmentDescription sourceEnvironment = lookupEnvironment(applicationName,
                                                                 ensureSuffix(
                                                                     sourceEnvironmentCNamePrefix));
    EnvironmentDescription targetEnvironment = lookupEnvironment(applicationName,
                                                                 ensureSuffix(
                                                                     targetEnvironmentCNamePrefix));

    SwapCNamesContext context = SwapCNamesContextBuilder
        .swapCNamesContext()//
        .withSourceEnvironmentId(sourceEnvironment.getEnvironmentId())//
        .withSourceEnvironmentName(
            sourceEnvironment.getEnvironmentName())//
        .withDestinationEnvironmentId(
            targetEnvironment.getEnvironmentId())//
        .withDestinationEnvironmentName(
            targetEnvironment.getEnvironmentName())//
        .build();

    SwapCNamesCommand command = new SwapCNamesCommand(this);

    return command.execute(context);

  }
}

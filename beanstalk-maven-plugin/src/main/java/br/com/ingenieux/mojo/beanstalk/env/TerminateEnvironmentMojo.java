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

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContextBuilder;

/**
 * Terminates the Environment
 *
 * See the docs for <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_TerminateEnvironment.html"
 * >TerminateEnvironment API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.1.0
 */
@Mojo(name = "terminate-environment")
public class TerminateEnvironmentMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * Terminate resources as well?
   */
  @Parameter(property = "beanstalk.terminateResources", defaultValue = "true")
  boolean terminateResources;

  @Override
  protected Object executeInternal() throws AbstractMojoExecutionException {
    TerminateEnvironmentContext context = TerminateEnvironmentContextBuilder
        .terminateEnvironmentContext().withEnvironmentId(curEnv.getEnvironmentId())
        .withEnvironmentName(curEnv.getEnvironmentName())
        .withTerminateResources(terminateResources).build();
    TerminateEnvironmentCommand command = new TerminateEnvironmentCommand(this);

    return command.execute(context);
  }

}

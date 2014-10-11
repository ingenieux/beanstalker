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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

/**
 * Waits for Environment Status to Change
 *
 * @since 0.2.2
 */
@Mojo(name = "wait-for-environment")
public class WaitForEnvironmentMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  /**
   * Minutes until timeout
   */
  @Parameter(property = "beanstalk.timeoutMins", defaultValue = "20")
  Integer timeoutMins;

  /**
   * Status to Wait For
   */
  @Parameter(property = "beanstalk.statusToWaitFor", defaultValue = "Ready")
  String statusToWaitFor;

  /**
   * Health to Wait For
   */
  @Parameter(property = "beanstalk.healthToWaitFor", defaultValue = "Green")
  String healthToWaitFor;

  /**
   * Environment Ref
   */
  @Parameter(property = "beanstalk.environmentRef",
             defaultValue = "${project.artifactId}.elasticbeanstalk.com")
  String environmentRef;

  @Override
  protected Object executeInternal() throws Exception {
    WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
        .withApplicationName(applicationName)//
        .withStatusToWaitFor(statusToWaitFor)//
        .withTimeoutMins(timeoutMins)//
        .withHealth(healthToWaitFor)//
        .withEnvironmentRef(environmentRef)//
        .build();

    WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);

    return command.execute(context);
  }
}

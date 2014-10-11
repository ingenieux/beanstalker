package br.com.ingenieux.mojo.beanstalk.bg;

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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Collection;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import static java.lang.String.format;

/**
 * <p>Implements Bluegreen Deployment</p>
 *
 * @since 1.3.0
 */
@Mojo(name = "blue-green")
public class BluegreenDeploymentMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * environmentNamePrefix - Matches all environment names prefixed with
   */
  @Parameter(property = "beanstalk.environmentNamePrefix", required = true,
             defaultValue = "${beanstalk.environmentName}")
  protected String environmentNamePrefix;
  /**
   * Version Label to use. Defaults to Project Version
   */
  @Parameter(property = "beanstalk.versionLabel", defaultValue = "${project.version}")
  String versionLabel;

  @Override
  protected Object executeInternal() throws Exception {
    versionLabel = lookupVersionLabel(applicationName, versionLabel);

    getLog().info(format("Using version %s", versionLabel));

    Collection<EnvironmentDescription>
        envs =
        new WaitForEnvironmentCommand(this).lookupInternal(
            new WaitForEnvironmentContextBuilder().withApplicationName(applicationName)
                .withEnvironmentRef(environmentNamePrefix + "*").build());

    if (envs.size() > 2) {
      final Collection<String>
          environmentList =
          Collections2.transform(envs, new Function<EnvironmentDescription, String>() {
            @Override
            public String apply(EnvironmentDescription input) {
              return format("%s[%s]", input.getEnvironmentId(), input.getEnvironmentName());
            }
          });

      String
          message =
          "Ooops. There are multiple environments matching the lookup spec: " + environmentList;

      getLog().warn(message);
      getLog().warn("Will pick one at random anyway as long as it uses WebServer tier name");
    }

    String otherEnvId = null;
    for (EnvironmentDescription e : envs) {
      if (!e.getEnvironmentId().equals(curEnv.getEnvironmentId()) && "WebServer"
          .equals(e.getTier().getName())) {
        otherEnvId = e.getEnvironmentId();
        break;
      }
    }

    getLog().info(format(
        "(Green) Environment with environmentId['%s'] will be prepared once its ready to update",
        curEnv.getEnvironmentId()));

    new WaitForEnvironmentCommand(this).execute(
        new WaitForEnvironmentContextBuilder().withStatusToWaitFor("Ready")
            .withApplicationName(applicationName).withEnvironmentRef(curEnv.getEnvironmentId())
            .build());

    getLog().info(format(
        "(Blue) Environment with environmentId['%s'] will be prepared once its ready to update",
        otherEnvId));

    new WaitForEnvironmentCommand(this).execute(
        new WaitForEnvironmentContextBuilder().withStatusToWaitFor("Ready")
            .withApplicationName(applicationName).withEnvironmentRef(otherEnvId).build());

    getLog().info(format("(Blue) Updating environmentId to version %s", versionLabel));

    new UpdateEnvironmentCommand(this).execute(
        new UpdateEnvironmentContextBuilder().withEnvironmentId(otherEnvId)
            .withVersionLabel(versionLabel).build());

    getLog().info(
        format("(Blue) Waiting for environmentId['%s'] to get ready and green prior to switching",
               otherEnvId));

    new WaitForEnvironmentCommand(this).execute(
        new WaitForEnvironmentContextBuilder().withStatusToWaitFor("Ready")
            .withApplicationName(applicationName).withHealth("Green").withEnvironmentRef(otherEnvId)
            .build());

    getLog().info(format("Ok. Switching"));

    getService().swapEnvironmentCNAMEs(
        new SwapEnvironmentCNAMEsRequest().withDestinationEnvironmentId(curEnv.getEnvironmentId())
            .withSourceEnvironmentId(otherEnvId));

    getLog().info(format("Done."));

    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}

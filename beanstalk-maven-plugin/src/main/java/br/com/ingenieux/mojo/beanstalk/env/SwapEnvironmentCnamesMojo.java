/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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


import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.util.EnvironmentHostnameUtil;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;

import java.util.Collection;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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
   * EnvironmentRef of source environment
   */
  @Parameter(property = "beanstalk.sourceEnvironmentRef")
  String sourceEnvironmentRef;

  /**
   * EnvironmentRef of target environment
   */
  @Parameter(property = "beanstalk.targetEnvironmentRef")
  String targetEnvironmentRef;

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

    boolean nonBlank(String... args) {
        for (String a : args)
          if (isBlank(a))
            return false;

        return true;
    }

  @Override
  protected Object executeInternal() throws AbstractMojoExecutionException {
      SwapCNamesContext context;

      if (nonBlank(sourceEnvironmentRef, targetEnvironmentRef)) {
          EnvironmentDescription e0 = findOnly(new WaitForEnvironmentCommand(this).
                  lookupInternal(
                          new WaitForEnvironmentContextBuilder().
                                  withApplicationName(applicationName).
                                  withEnvironmentRef(sourceEnvironmentRef).
                                  build()));

          EnvironmentDescription e1 = findOnly(new WaitForEnvironmentCommand(this).
                  lookupInternal(
                          new WaitForEnvironmentContextBuilder().
                                  withApplicationName(applicationName).
                                  withEnvironmentRef(targetEnvironmentRef).
                                  build()));

          Validate.isTrue(isNotBlank(e0.getCNAME()), format("Environment '%s' must be non-worker (thus having a cname)", e1.getEnvironmentId()));
          Validate.isTrue(isNotBlank(e1.getCNAME()), format("Environment '%s' must be non-worker (thus having a cname)", e1.getEnvironmentId()));

          context = SwapCNamesContextBuilder
                  .swapCNamesContext()//
                  .withSourceEnvironmentId(e0.getEnvironmentId())//
                  .withDestinationEnvironmentId(
                          e1.getEnvironmentId())//
                  .build();
      } else if (nonBlank(sourceEnvironmentCNamePrefix, targetEnvironmentCNamePrefix)){
          sourceEnvironmentCNamePrefix = defaultString(sourceEnvironmentCNamePrefix);
          targetEnvironmentCNamePrefix = defaultString(targetEnvironmentCNamePrefix);

          Validate.isTrue(sourceEnvironmentCNamePrefix.matches("[\\p{Alnum}\\-]{4,63}"), "Invalid Source Environment CName Prefix: " + sourceEnvironmentCNamePrefix);
          Validate.isTrue(targetEnvironmentCNamePrefix.matches("[\\p{Alnum}\\-]{4,63}"), "Invalid Target Environment CName Prefix: " + targetEnvironmentCNamePrefix);

          EnvironmentDescription sourceEnvironment = lookupEnvironment(applicationName, EnvironmentHostnameUtil.ensureSuffix(sourceEnvironmentCNamePrefix, getRegion()));
          EnvironmentDescription targetEnvironment = lookupEnvironment(applicationName, EnvironmentHostnameUtil.ensureSuffix(targetEnvironmentCNamePrefix, getRegion()));

          // TODO: Why 'destination' instead of 'target'? Make it consistent
          context = SwapCNamesContextBuilder
                  .swapCNamesContext()//
                  .withSourceEnvironmentId(sourceEnvironment.getEnvironmentId())//
                  .withDestinationEnvironmentId(
                          targetEnvironment.getEnvironmentId())//
                  .build();
      } else {
          throw new IllegalArgumentException("You must supply either {source,target}EnvironmentCNamePrefix or {source,target}EnvironmentRef");
      }

      SwapCNamesCommand command = new SwapCNamesCommand(this);

    return command.execute(context);

  }

    private EnvironmentDescription findOnly(Collection<EnvironmentDescription> environmentDescriptions) {
        Validate.isTrue(1 == environmentDescriptions.size(), "More than one environment matches spec");

        return environmentDescriptions.iterator().next();
    }
}

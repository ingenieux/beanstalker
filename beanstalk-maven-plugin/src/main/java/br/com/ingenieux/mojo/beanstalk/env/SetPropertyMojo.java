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

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentResult;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Arrays;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Sets a System Property in a Running Environment
 *
 * @since 1.1.0
 */
@Mojo(name = "set-property", requiresDirectInvocation = true)
public class SetPropertyMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * System Property Name
   */
  @Parameter(property = "beanstalk.envName", required = true)
  String envName;

  /**
   * System Property Value
   */
  @Parameter(property = "beanstalk.envValue", required = true)
  String envValue;

  protected Object executeInternal() throws Exception {
    waitForNotUpdating();

    UpdateEnvironmentRequest req = new UpdateEnvironmentRequest()
        .withEnvironmentId(curEnv.getEnvironmentId());

    req.setOptionSettings(Arrays.asList(new ConfigurationOptionSetting()
                                            .withNamespace(
                                                "aws:elasticbeanstalk:application:environment")
                                            .withOptionName(envName).withValue(envValue)));

    UpdateEnvironmentResult result = getService().updateEnvironment(req);

    return result;
  }
}

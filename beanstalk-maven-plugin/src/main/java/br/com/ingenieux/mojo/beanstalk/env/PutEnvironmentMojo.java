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

import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContextBuilder;

/**
 * Creates (if needed) or Updates an Elastic Beanstalk Environment
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html"
 * >CreateEnvironment API</a> call.
 *
 * @since 0.2.8
 */
@Mojo(name = "put-environment")
public class PutEnvironmentMojo extends CreateEnvironmentMojo {

  @Override
  protected void configure() {
    try {
      curEnv = super.lookupEnvironment(applicationName, environmentRef);
    } catch (Exception exc) {
      // Previous Environment Does Not Exists. So its fine to just create the new environment.
    }
  }

  @Override
  protected Object executeInternal() throws Exception {
                /*
		 * We *DO* have an existing environment. So we're just calling update-environment instead
		 */
    if (null != curEnv) {
      UpdateEnvironmentContext context = UpdateEnvironmentContextBuilder
          .updateEnvironmentContext()
          .withEnvironmentId(curEnv.getEnvironmentId())//
          .withVersionLabel(versionLabel)//
          .build();

      UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(
          this);

      return command.execute(context);
    }

    return super.executeInternal();
  }
}

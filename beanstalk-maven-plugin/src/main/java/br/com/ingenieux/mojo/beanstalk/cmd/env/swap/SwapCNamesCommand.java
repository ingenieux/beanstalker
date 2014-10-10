package br.com.ingenieux.mojo.beanstalk.cmd.env.swap;

import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;

import org.apache.maven.plugin.AbstractMojoExecutionException;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;

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
public class SwapCNamesCommand extends BaseCommand<SwapCNamesContext, Object> {

  /**
   * Constructor
   *
   * @param parentMojo parent mojo
   */
  public SwapCNamesCommand(AbstractBeanstalkMojo parentMojo) throws AbstractMojoExecutionException {
    super(parentMojo);
  }

  @Override
  protected Object executeInternal(SwapCNamesContext context) throws Exception {
    SwapEnvironmentCNAMEsRequest request = new SwapEnvironmentCNAMEsRequest();

    request.setSourceEnvironmentName(context.getSourceEnvironmentName());

    request.setSourceEnvironmentId(context.getSourceEnvironmentId());

    request
        .setDestinationEnvironmentName(context.getDestinationEnvironmentName());

    request.setDestinationEnvironmentId(context.getDestinationEnvironmentId());

    service.swapEnvironmentCNAMEs(request);

    return request;
  }
}

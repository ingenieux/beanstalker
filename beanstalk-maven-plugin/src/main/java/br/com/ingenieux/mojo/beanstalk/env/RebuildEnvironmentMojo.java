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

import com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Rebuilds an Environment
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RebuildEnvironment.html"
 * >RebuildEnvironment API</a> call.
 *
 * @author Aldrin Leal
 * @since 0.1.0
 */
@Mojo(name = "rebuild-environment")
public class RebuildEnvironmentMojo extends AbstractNeedsEnvironmentMojo {

  @Override
  protected Object executeInternal() throws AbstractMojoExecutionException {
    RebuildEnvironmentRequest req = new RebuildEnvironmentRequest();

    req.setEnvironmentId(curEnv.getEnvironmentId());
    req.setEnvironmentName(curEnv.getEnvironmentName());

    getService().rebuildEnvironment(req);

    return null;
  }

}

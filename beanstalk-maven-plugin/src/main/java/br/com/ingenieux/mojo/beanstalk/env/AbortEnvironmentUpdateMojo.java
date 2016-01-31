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

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import com.amazonaws.services.elasticbeanstalk.model.AbortEnvironmentUpdateRequest;
import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Aborts Environment Update
 *
 * See the docs for the <a href= "http://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_AbortEnvironmentUpdate.html"
 * >AbortEnvironmentUpdate API</a> call.
 *
 * @author Aldrin Leal
 * @since 1.5.0
 */
@Mojo(name = "abort-environment-update")
public class AbortEnvironmentUpdateMojo extends AbstractNeedsEnvironmentMojo {

  @Override
  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    AbortEnvironmentUpdateRequest req = new AbortEnvironmentUpdateRequest();

    req.setEnvironmentId(curEnv.getEnvironmentId());
    req.setEnvironmentName(curEnv.getEnvironmentName());

    super.configure();

    return curEnv;
  }
}

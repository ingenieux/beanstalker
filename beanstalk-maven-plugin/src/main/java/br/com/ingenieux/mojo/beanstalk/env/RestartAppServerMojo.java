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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;

/**
 * Restarts the Application Server
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RestartAppServer.html"
 * >RestartAppServer API</a> call.
 * 
 * @author Aldrin Leal
 * 
 */
@MojoGoal("restart-application-server")
@MojoSince("0.1.0")
public class RestartAppServerMojo extends AbstractNeedsEnvironmentMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		RestartAppServerRequest req = new RestartAppServerRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);

		getService().restartAppServer(req);

		return null;
	}
}

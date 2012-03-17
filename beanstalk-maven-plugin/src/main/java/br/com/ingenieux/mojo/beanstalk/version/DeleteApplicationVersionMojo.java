package br.com.ingenieux.mojo.beanstalk.version;

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
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;

/**
 * Deletes an Application Version
 * 
 * See the docs for <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DeleteApplicationVersion.html"
 * >DeleteApplicationVersion API</a> call.
 * 
 * @author Aldrin Leal
 */
@MojoGoal("delete-application-version")
@MojoSince("0.1.0")
public class DeleteApplicationVersionMojo extends AbstractBeanstalkMojo {
	@MojoParameter(expression="${beanstalk.applicationName}", defaultValue="${project.artifactId}", required=true, description="Beanstalk Application Name")
	protected String applicationName;

	@MojoParameter(expression="${beanstalk.versionLabel}", defaultValue="${project.version}")
	String versionLabel;

	/**
	 * Delete the source bundle?
	 */
	@MojoParameter(expression="${beanstalk.deleteSourceBundle}", defaultValue="false")
	private boolean deleteSourceBundle;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DeleteApplicationVersionRequest req = new DeleteApplicationVersionRequest();

		req.setApplicationName(applicationName);
		req.setDeleteSourceBundle(deleteSourceBundle);
		req.setVersionLabel(versionLabel);

		getService().deleteApplicationVersion(req);

		return null;
	}

}

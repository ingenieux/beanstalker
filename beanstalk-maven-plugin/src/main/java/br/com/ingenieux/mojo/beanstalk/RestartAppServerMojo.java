package br.com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;

/**
 * Restarts the Application Server
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RestartAppServer.html"
 * >RestartAppServer API</a> call.
 * 
 * @goal restart-application-server
 * @author Aldrin Leal
 * 
 */
public class RestartAppServerMojo extends AbstractBeanstalkMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		RestartAppServerRequest req = new RestartAppServerRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);

		service.restartAppServer(req);

		return null;
	}
}

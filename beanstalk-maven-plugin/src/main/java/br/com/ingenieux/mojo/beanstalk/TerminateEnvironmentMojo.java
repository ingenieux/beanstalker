package br.com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;

/**
 * Terminates the Environment
 * 
 * See the docs for <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_TerminateEnvironment.html"
 * >TerminateEnvironment API</a> call.
 * 
 * @author Aldrin Leal
 * @goal terminate-environment
 */
public class TerminateEnvironmentMojo extends AbstractBeanstalkMojo {
	/**
	 * Terminate resources as well?
	 * 
	 * @parameter expr="${terminateResources}"
	 */
	boolean terminateResources = true;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		TerminateEnvironmentRequest req = new TerminateEnvironmentRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);
		req.setTerminateResources(terminateResources);

		return service.terminateEnvironment(req);
	}

}

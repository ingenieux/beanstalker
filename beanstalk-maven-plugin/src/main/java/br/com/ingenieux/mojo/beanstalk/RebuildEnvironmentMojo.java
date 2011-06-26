package br.com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest;

/**
 * Rebuilds an Environment
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_RebuildEnvironment.html"
 * >RebuildEnvironment API</a> call.
 * 
 * @author Aldrin Leal
 * @goal rebuild-environment
 */
public class RebuildEnvironmentMojo extends AbstractBeanstalkMojo {

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		RebuildEnvironmentRequest req = new RebuildEnvironmentRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);

		service.rebuildEnvironment(req);

		return null;
	}

}

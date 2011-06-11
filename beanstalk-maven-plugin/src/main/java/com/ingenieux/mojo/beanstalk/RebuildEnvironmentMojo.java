package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.RebuildEnvironmentRequest;

/**
 * Terminates the Environment
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

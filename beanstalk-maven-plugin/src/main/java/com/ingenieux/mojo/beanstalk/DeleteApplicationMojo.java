package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;

/**
 * Deletes an Application
 * 
 * @goal delete-application
 * @author Aldrin Leal
 * 
 */
public class DeleteApplicationMojo extends AbstractBeanstalkMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DeleteApplicationRequest req = new DeleteApplicationRequest();

		req.setApplicationName(applicationName);

		service.deleteApplication(req);

		return null;
	}
}

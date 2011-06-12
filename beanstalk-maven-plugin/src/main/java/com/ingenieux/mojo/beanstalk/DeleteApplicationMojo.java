package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;

/**
 * Deletes an Application
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DeleteApplication.html"
 * >DeleteApplication API</a> call.
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

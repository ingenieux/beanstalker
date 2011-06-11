package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.CreateStorageLocationResult;

/**
 * Creates a Storage Location (for logs)
 * 
 * @goal create-storage-location
 * 
 * @author Aldrin Leal
 * 
 */
public class CreateStorageLocationMojo extends AbstractBeanstalkMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		CreateStorageLocationResult result = service.createStorageLocation();

		return result;
	}
}

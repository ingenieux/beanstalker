package br.com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.CreateStorageLocationResult;

/**
 * Creates a Storage Location (for logs)
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateStorageLocation.html"
 * >CreateStorageLocation API</a> call.
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

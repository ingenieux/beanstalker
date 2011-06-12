package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;

/**
 * Describe running environments
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeEnvironments.html"
 * >DescribeEnvironments API</a> call.
 * 
 * @author Aldrin Leal
 * @goal describe-environments
 */
public class DescribeEnvironmentsMojo extends AbstractBeanstalkMojo {
	/**
	 * Include Deleted?
	 * 
	 * @parameter expr="${includeDeleted}"
	 */
	boolean includeDeleted = true;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest();

		req.setApplicationName(applicationName);
		req.setIncludeDeleted(includeDeleted);
		req.setVersionLabel(versionLabel);

		return service.describeEnvironments(req);
	}

}

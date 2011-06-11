package com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Lists the available solution stacks
 * 
 * @goal list-stacks
 * @author Aldrin Leal
 * 
 */
public class ListStacksMojo extends AbstractBeanstalkMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		return service.listAvailableSolutionStacks();
	}

}

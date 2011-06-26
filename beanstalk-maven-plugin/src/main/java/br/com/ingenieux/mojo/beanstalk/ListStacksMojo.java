package br.com.ingenieux.mojo.beanstalk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Lists the available solution stacks
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_ListAvailableSolutionStacks.html"
 * >ListAvailableSolutionStacks API</a> call.
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

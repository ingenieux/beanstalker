package br.com.ingenieux.mojo.beanstalk.env;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/**
 * Launches a new environment and, when done, replace with the existing,
 * terminating when needed. It combines both create-environment,
 * wait-for-environment, swap-environment-cnames, and terminate-environment
 * 
 * @goal replace-environment
 */
public class ReplaceEnvironmentMojo extends CreateEnvironmentMojo {
	/**
	 * Minutes until timeout
	 * 
	 * @parameter expression="${beanstalk.timeoutMins}" default-value="20"
	 */
	Integer timeoutMins;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		/*
		 * Is the desired cname not being used by other environments? If so, just
		 * launch the environment
		 */
		if (!hasEnvironmentFor(applicationName, cnamePrefix)) {
			if (getLog().isInfoEnabled())
				getLog().info("Just launching a new environment.");

			return super.executeInternal();
		}

		/*
		 * Gets the current environment using this cname
		 */
		EnvironmentDescription curEnv = getEnvironmentFor(applicationName,
		    cnamePrefix);

		/*
		 * Decides on a cnamePrefix, and launches a new environment
		 */
		String cnamePrefixToCreate = getCNamePrefixToCreate();

		if (getLog().isInfoEnabled())
			getLog().info(
			    "Creating a new environment on " + cnamePrefixToCreate
			        + ".elasticbeanstalk.com");

		CreateEnvironmentResult createEnvResult = createEnvironment(cnamePrefixToCreate);

		/*
		 * Waits for completion
		 */
		EnvironmentDescription newEnvDesc = null;

		try {
			newEnvDesc = waitForEnvironment(createEnvResult.getEnvironmentId());
		} catch (Exception exc) {
			/*
			 * Terminates the failed launched environment
			 */
			terminateAndWaitForEnvironment(createEnvResult.getEnvironmentId());

			handleException(exc);

			return null;
		}

		/*
		 * Swaps
		 */
		swapEnvironmentCNames(newEnvDesc.getEnvironmentId(),
		    curEnv.getEnvironmentId());

		/*
		 * Terminates the previous environment - and waits for it
		 */
		terminateAndWaitForEnvironment(curEnv.getEnvironmentId());

		/*
		 * Now renames into the expected name
		 */
		renameEnvironment(newEnvDesc.getEnvironmentId(),
		    curEnv.getEnvironmentName());

		return createEnvResult;
	}

	private void renameEnvironment(String environmentId, String environmentName)
	    throws MojoFailureException, MojoExecutionException {
		getLog().info(
		    "Renaming environmentName from environmentId " + environmentId
		        + " to environmentName" + environmentName);

		UpdateEnvironmentContext context = UpdateEnvironmentContextBuilder
		    .updateEnvironmentContext().withEnvironmentId(environmentId)//
		    .withEnvironmentName(environmentName)//
		    .withOptionSettings(optionSettings)//
		    .withTemplateName(templateName)//
		    .withVersionLabel(versionLabel)//
		    .build();
		UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(this);

		command.execute(context);
	}

	/**
	 * Swaps environment cnames
	 * 
	 * @param newEnvironmentId
	 *          environment id
	 * @param curEnvironmentId
	 *          environment id
	 * @throws MojoFailureException
	 *           failure
	 * @throws MojoExecutionException
	 *           failure
	 */
	protected void swapEnvironmentCNames(String newEnvironmentId,
	    String curEnvironmentId) throws MojoFailureException,
	    MojoExecutionException {
		getLog().info(
		    "Swapping environment cnames " + newEnvironmentId + " and "
		        + curEnvironmentId);

		SwapCNamesContext context = SwapCNamesContextBuilder.swapCNamesContext()//
		    .withSourceEnvironmentId(newEnvironmentId)//
		    .withDestinationEnvironmentId(curEnvironmentId)//
		    .build();
		SwapCNamesCommand command = new SwapCNamesCommand(this);

		command.execute(context);
	}

	/**
	 * Terminates and waits for an environment
	 * 
	 * @param environmentId
	 *          environment id to terminate
	 * @throws MojoFailureException
	 *           Failure
	 * @throws MojoExecutionException
	 *           Failure
	 */
	protected void terminateAndWaitForEnvironment(String environmentId)
	    throws MojoFailureException, MojoExecutionException {
		{
			getLog().info("Terminating environmentId=" + environmentId);

			TerminateEnvironmentContext terminatecontext = new TerminateEnvironmentContextBuilder()
			    .withEnvironmentId(environmentId).withTerminateResources(true)
			    .build();
			TerminateEnvironmentCommand command = new TerminateEnvironmentCommand(
			    this);

			command.execute(terminatecontext);
		}

		{
			WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
			    .withApplicationName(applicationName)
			    .withEnvironmentId(environmentId).withStatusToWaitFor("Terminated")
			    .withTimeoutMins(timeoutMins).build();

			WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);

			command.execute(context);
		}
	}

	/**
	 * Waits for an environment to get ready. Throws an exception either if this
	 * environment couldn't get into Ready state or there was a timeout
	 * 
	 * @param environmentId
	 *          environmentId to wait for
	 * @return EnvironmentDescription in Ready state
	 * @throws MojoFailureException
	 *           Failure
	 * @throws MojoExecutionException
	 *           Failure
	 */
	protected EnvironmentDescription waitForEnvironment(String environmentId)
	    throws MojoFailureException, MojoExecutionException {
		getLog().info("Waiting for environmentId " + environmentId + " to get into Ready state");
		
		WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
		    .withApplicationName(applicationName).withStatusToWaitFor("Ready").withEnvironmentId(environmentId)
		    .withTimeoutMins(timeoutMins).build();

		WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);

		return command.execute(context);
	}

	/**
	 * Creates a cname prefix if needed, or returns the desired one
	 * 
	 * @return cname prefix to launch environment into
	 */
	protected String getCNamePrefixToCreate() {
		String cnamePrefixToReturn = cnamePrefix;
		int i = 0;

		while (hasEnvironmentFor(applicationName, cnamePrefixToReturn))
			cnamePrefixToReturn = String.format("%s-%d", cnamePrefix, i++);

		return cnamePrefixToReturn;
	}

	/**
	 * Boolean predicate for environment existence
	 * 
	 * @param applicationName
	 *          application name
	 * @param cnamePrefix
	 *          cname prefix
	 * @return true if the application name has this cname prefix
	 */
	protected boolean hasEnvironmentFor(String applicationName, String cnamePrefix) {
		return null != getEnvironmentFor(applicationName, cnamePrefix);
	}

	/**
	 * Returns the environment description matching applicationName and
	 * cnamePrefix
	 * 
	 * @param applicationName
	 *          application name
	 * @param cnamePrefix
	 *          cname prefix
	 * @return environment description
	 */
	protected EnvironmentDescription getEnvironmentFor(String applicationName,
	    String cnamePrefix) {
		Collection<EnvironmentDescription> environments = getEnvironmentsFor(applicationName);
		String cnameToMatch = String.format("%s.elasticbeanstalk.com", cnamePrefix);

		/*
		 * Finds a matching environment
		 */
		for (EnvironmentDescription envDesc : environments)
			if (envDesc.getCNAME().equals(cnameToMatch))
				return envDesc;

		return null;
	}

	@Override
	protected String getEnvironmentName(String environmentName) {
		String resultingEnvironmentName = environmentName;

		int i = 0;

		while (containsNamedEnvironment(resultingEnvironmentName))
			resultingEnvironmentName = String.format("%s-%d", environmentName, i++);

		return resultingEnvironmentName;
	}

	/**
	 * Boolean predicate for named environment
	 * 
	 * @param environmentName
	 *          environment name
	 * @return true if environment name exists
	 */
	protected boolean containsNamedEnvironment(String environmentName) {
		for (EnvironmentDescription envDesc : getEnvironmentsFor(applicationName))
			if (envDesc.getEnvironmentName().equals(environmentName))
				return true;

		return false;
	}
}

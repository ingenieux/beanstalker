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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/**
 * Launches a new environment and, when done, replace with the existing,
 * terminating when needed. It combines both create-environment,
 * wait-for-environment, swap-environment-cnames, and terminate-environment
 * 
 * @since 0.2.0
 */
@Mojo(name="replace-environment")
// Best Guess Evar
public class ReplaceEnvironmentMojo extends CreateEnvironmentMojo {
	/**
	 * 
	 */
	private static final Pattern PATTERN_NUMBERED = Pattern
			.compile("^(.*)-(\\d+)$");

	/**
	 * Max Environment Name
	 */
	private static final int MAX_ENVNAME_LEN = 23;

	/**
	 * Minutes until timeout
	 */
	@Parameter(property="beanstalk.timeoutMins", defaultValue = "20")
	Integer timeoutMins;

	@Override
	protected EnvironmentDescription handleResults(String kind,
			List<EnvironmentDescription> environments)
			throws MojoExecutionException {
		// Don't care - We're an exception to the rule, you know.

		return null;
	}

	@Override
	protected Object executeInternal() throws AbstractMojoExecutionException {
		/*
		 * Is the desired cname not being used by other environments? If so,
		 * just launch the environment
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

		copyOptionSettings(curEnv);

		String newEnvironmentName = getNewEnvironmentName(this.environmentName);

		if (getLog().isInfoEnabled())
			getLog().info("And it'll be named " + newEnvironmentName);

		CreateEnvironmentResult createEnvResult = createEnvironment(
				cnamePrefixToCreate, newEnvironmentName);

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
				curEnv.getEnvironmentId(), cnamePrefix);

		/*
		 * Terminates the previous environment - and waits for it
		 */
		terminateAndWaitForEnvironment(curEnv.getEnvironmentId());

		return createEnvResult;
	}

	/**
	 * Prior to Launching a New Environment, lets look and copy the most we can
	 * 
	 * @param curEnv
	 *            current environment
	 */
	private void copyOptionSettings(EnvironmentDescription curEnv) {
		/**
		 * Skip if we don't have anything
		 */
		if (null != this.optionSettings && this.optionSettings.length > 0)
			return;

		DescribeConfigurationSettingsResult configSettings = getService()
				.describeConfigurationSettings(
						new DescribeConfigurationSettingsRequest()
								.withApplicationName(applicationName)
								.withEnvironmentName(
										curEnv.getEnvironmentName()));

		List<ConfigurationOptionSetting> newOptionSettings = new ArrayList<ConfigurationOptionSetting>(
				configSettings.getConfigurationSettings().get(0)
						.getOptionSettings());

		ListIterator<ConfigurationOptionSetting> listIterator = newOptionSettings
				.listIterator();

		while (listIterator.hasNext()) {
			ConfigurationOptionSetting curOptionSetting = listIterator.next();

			/*
			 * Filters out harmful options
			 * 
			 * I really mean harmful - If you mention a terminated environment
			 * settings, Elastic Beanstalk will accept, but this might lead to
			 * inconsistent states, specially when creating / listing environments. 
			 * 
			 * Trust me on this one.
			 */
			boolean bInvalid = isBlank(curOptionSetting.getValue());

			if (!bInvalid)
				bInvalid |= (curOptionSetting.getNamespace().equals(
						"aws:cloudformation:template:parameter") && curOptionSetting
						.getOptionName().equals("AppSource"));

			if (!bInvalid)
				bInvalid |= (curOptionSetting.getValue().contains(curEnv
						.getEnvironmentId()));

			if (bInvalid)
				listIterator.remove();
		}

		/*
		 * Then copy it back
		 */
		this.optionSettings = newOptionSettings
				.toArray(new ConfigurationOptionSetting[newOptionSettings
						.size()]);
	}

	/**
	 * Swaps environment cnames
	 * 
	 * @param newEnvironmentId
	 *            environment id
	 * @param curEnvironmentId
	 *            environment id
	 * @param cnamePrefix
	 * @throws AbstractMojoExecutionException
	 */
	protected void swapEnvironmentCNames(String newEnvironmentId,
			String curEnvironmentId, String cnamePrefix)
			throws AbstractMojoExecutionException {
		getLog().info(
				"Swapping environment cnames " + newEnvironmentId + " and "
						+ curEnvironmentId);

		{
			SwapCNamesContext context = SwapCNamesContextBuilder
					.swapCNamesContext()//
					.withSourceEnvironmentId(newEnvironmentId)//
					.withDestinationEnvironmentId(curEnvironmentId)//
					.build();
			SwapCNamesCommand command = new SwapCNamesCommand(this);

			command.execute(context);
		}

		{
			WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
					.withApplicationName(applicationName)//
					.withStatusToWaitFor("Ready")//
					.withEnvironmentId(newEnvironmentId)//
					.withTimeoutMins(timeoutMins)//
					.withDomainToWaitFor(cnamePrefix).build();

			WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(
					this);

			command.execute(context);
		}
	}

	/**
	 * Terminates and waits for an environment
	 * 
	 * @param environmentId
	 *            environment id to terminate
	 * @throws AbstractMojoExecutionException
	 */
	protected void terminateAndWaitForEnvironment(String environmentId)
			throws AbstractMojoExecutionException {
		{
			getLog().info("Terminating environmentId=" + environmentId);

			TerminateEnvironmentContext terminatecontext = new TerminateEnvironmentContextBuilder()
					.withEnvironmentId(environmentId)
					.withTerminateResources(true).build();
			TerminateEnvironmentCommand command = new TerminateEnvironmentCommand(
					this);

			command.execute(terminatecontext);
		}

		{
			WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
					.withApplicationName(applicationName)
					.withEnvironmentId(environmentId)
					.withStatusToWaitFor("Terminated")
					.withTimeoutMins(timeoutMins).build();

			WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(
					this);

			command.execute(context);
		}
	}

	/**
	 * Waits for an environment to get ready. Throws an exception either if this
	 * environment couldn't get into Ready state or there was a timeout
	 * 
	 * @param environmentId
	 *            environmentId to wait for
	 * @return EnvironmentDescription in Ready state
	 * @throws AbstractMojoExecutionException
	 */
	protected EnvironmentDescription waitForEnvironment(String environmentId)
			throws AbstractMojoExecutionException {
		getLog().info(
				"Waiting for environmentId " + environmentId
						+ " to get into Ready state");

		WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
				.withApplicationName(applicationName)
				.withStatusToWaitFor("Ready").withEnvironmentId(environmentId)
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
	 *            application name
	 * @param cnamePrefix
	 *            cname prefix
	 * @return true if the application name has this cname prefix
	 */
	protected boolean hasEnvironmentFor(String applicationName,
			String cnamePrefix) {
		return null != getEnvironmentFor(applicationName, cnamePrefix);
	}

	/**
	 * Returns the environment description matching applicationName and
	 * cnamePrefix
	 * 
	 * @param applicationName
	 *            application name
	 * @param cnamePrefix
	 *            cname prefix
	 * @return environment description
	 */
	protected EnvironmentDescription getEnvironmentFor(String applicationName,
			String cnamePrefix) {
		Collection<EnvironmentDescription> environments = getEnvironmentsFor(applicationName);
		String cnameToMatch = String.format("%s.elasticbeanstalk.com",
				cnamePrefix);

		/*
		 * Finds a matching environment
		 */
		for (EnvironmentDescription envDesc : environments)
			if (envDesc.getCNAME().equals(cnameToMatch))
				return envDesc;

		return null;
	}

	private String getNewEnvironmentName(String newEnvironmentName) {
		String result = newEnvironmentName;
		String environmentRadical = result;

		int i = 0;

		{
			Matcher matcher = PATTERN_NUMBERED.matcher(newEnvironmentName);

			if (matcher.matches()) {
				environmentRadical = matcher.group(1);

				i = 1 + Integer.valueOf(matcher.group(2));
			}
		}

		while (containsNamedEnvironment(result))
			result = formatAndTruncate("%s-%d", MAX_ENVNAME_LEN,
					environmentRadical, i++);

		return result;
	}

	/**
	 * Elastic Beanstalk Contains a Max EnvironmentName Limit. Lets truncate it,
	 * shall we?
	 * 
	 * @param mask
	 *            String.format Mask
	 * @param maxLen
	 *            Maximum Length
	 * @param args
	 *            String.format args
	 * @return formatted String, or maxLen rightmost characters
	 */
	protected String formatAndTruncate(String mask, int maxLen, Object... args) {
		String result = String.format(mask, args);

		if (result.length() > maxLen)
			result = result
					.substring(result.length() - maxLen, result.length());

		return result;
	}

	/**
	 * Boolean predicate for named environment
	 * 
	 * @param environmentName
	 *            environment name
	 * @return true if environment name exists
	 */
	protected boolean containsNamedEnvironment(String environmentName) {
		for (EnvironmentDescription envDesc : getEnvironmentsFor(applicationName))
			if (envDesc.getEnvironmentName().equals(environmentName))
				return true;

		return false;
	}
}

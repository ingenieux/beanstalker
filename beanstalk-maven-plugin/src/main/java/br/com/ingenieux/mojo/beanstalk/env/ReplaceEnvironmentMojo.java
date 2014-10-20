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

import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.ingenieux.mojo.aws.util.CredentialsUtil;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsContext;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.terminate.TerminateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Launches a new environment and, when done, replace with the existing, terminating when needed. It
 * combines both create-environment, wait-for-environment, swap-environment-cnames, and
 * terminate-environment
 *
 * @since 0.2.0
 */
@Mojo(name = "replace-environment")
// Best Guess Evar
public class ReplaceEnvironmentMojo extends CreateEnvironmentMojo {

  /**
   * Pattern for Increasing in Replace Environment
   */
  private static final Pattern PATTERN_NUMBERED = Pattern
      .compile("^(.*)-(\\d+)$");

  /**
   * Max Environment Name Length
   */
  private static final int MAX_ENVNAME_LEN = 23;

  /**
   * Minutes until timeout
   */
  @Parameter(property = "beanstalk.timeoutMins", defaultValue = "20")
  Integer timeoutMins;

  /**
   * Skips if Same Version?
   */
  @Parameter(property = "beanstalk.skipIfSameVersion", defaultValue = "true")
  boolean skipIfSameVersion = true;

  /**
   * Max Number of Attempts (for cnameSwap in Particular)
   */
  @Parameter(property = "beanstalk.maxAttempts", defaultValue = "15")
  Integer maxAttempts = 15;

  /**
   * Do a 'Mock' Terminate Environment Call (useful for Debugging)
   */
  @Parameter(property = "beanstalk.mockTerminateEnvironment", defaultValue = "false")
  boolean mockTerminateEnvironment = false;

  /**
   * Retry Interval, in Seconds
   */
  @Parameter(property = "beanstalk.attemptRetryInterval", defaultValue = "60")
  int attemptRetryInterval = 60;

  /**
   * <p>List of R53 Domains</p>
   *
   * <p>Could be set as either:</p> <ul> <li>fqdn:hostedZoneId (e.g. "services.modafocas.org:Z3DJ4DL0DIEEJA")</li>
   * <li>hosted zone name - will be set to root. (e.g., "modafocas.org")</li> </ul>
   */
  @Parameter(property = "beanstalk.domains")
  String[] domains;

  /**
   * Whether or not to copy option settings from old environment when replacing
   */
  @Parameter(property = "beanstalk.copyOptionSettings", defaultValue = "true")
  boolean copyOptionSettings = true;

  @Override
  protected EnvironmentDescription handleResults(
      Collection<EnvironmentDescription> environments)
      throws MojoExecutionException {
    // Don't care - We're an exception to the rule, you know.

    return null;
  }

  @Override
  protected Object executeInternal() throws Exception {
    solutionStack = lookupSolutionStack(solutionStack);

    /*
     * Is the desired cname not being used by other environments? If so,
     * just launch the environment
     */
    if (!hasEnvironmentFor(applicationName, cnamePrefix)) {
      if (getLog().isInfoEnabled()) {
        getLog().info("Just launching a new environment.");
      }

      return super.executeInternal();
    }

		/*
                 * Gets the current environment using this cname
		 */
    EnvironmentDescription curEnv = getEnvironmentFor(applicationName,
                                                      cnamePrefix);

    if (curEnv.getVersionLabel().equals(versionLabel) && skipIfSameVersion) {
      getLog().warn(
          format("Environment is running version %s and skipIfSameVersion is true. Returning",
                 versionLabel));

      return null;
    }

		/*
                 * Decides on a environmentRef, and launches a new environment
		 */
    String cnamePrefixToCreate = getCNamePrefixToCreate();

    if (getLog().isInfoEnabled()) {
      getLog().info(
          "Creating a new environment on " + cnamePrefixToCreate
          + ".elasticbeanstalk.com");
    }

    if (copyOptionSettings) {

        copyOptionSettings(curEnv);

        if (!solutionStack.equals(curEnv.getSolutionStackName())) {
            if (getLog().isInfoEnabled()) {
                getLog().warn(
                        format(
                                "(btw, we're launching with solutionStack/ set to '%s' instead of the default ('%s'). "
                                        + "If this is not the case, then we kindly ask you to file a bug report on the mailing list :)",
                                curEnv.getSolutionStackName(), solutionStack));
            }

            solutionStack = curEnv.getSolutionStackName();
        }
    }

    String newEnvironmentName = getNewEnvironmentName(StringUtils
                                                          .defaultString(this.environmentName,
                                                                         curEnv
                                                                             .getEnvironmentName()));

    if (getLog().isInfoEnabled()) {
      getLog().info("And it'll be named " + newEnvironmentName);
    }

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
      terminateEnvironment(createEnvResult.getEnvironmentId());

      handleException(exc);

      return null;
    }

		/*
		 * Swaps. Due to beanstalker-25, we're doing some extra logic we
		 * actually woudln't want to.
		 */
    {
      boolean swapped = false;
      for (int i = 1; i <= maxAttempts; i++) {
        try {
          swapEnvironmentCNames(newEnvDesc.getEnvironmentId(),
                                curEnv.getEnvironmentId(), cnamePrefix, newEnvDesc);
          swapped = true;
          break;
        } catch (Throwable exc) {
          if (exc instanceof MojoFailureException) {
            exc = Throwable.class.cast(MojoFailureException.class
                                           .cast(exc).getCause());
          }

          getLog().warn(
              format("Attempt #%d/%d failed. Sleeping and retrying. Reason: %s (type: %s)",
                     i, maxAttempts, exc.getMessage(), exc.getClass()));

          sleepInterval(attemptRetryInterval);
        }
      }

      if (!swapped) {
        getLog().info(
            "Failed to properly Replace Environment. Finishing the new one. And throwing you a failure");

        terminateEnvironment(newEnvDesc.getEnvironmentId());

        String
            message =
            "Unable to swap cnames. btw, see https://github.com/ingenieux/beanstalker/issues/25 and help us improve beanstalker";

        getLog().warn(message);

        throw new MojoFailureException(message);
      }
    }

		/*
		 * Terminates the previous environment
		 */
    terminateEnvironment(curEnv.getEnvironmentId());

    return createEnvResult;
  }

  public void sleepInterval(int pollInterval) {
    getLog().info(
        format("Sleeping for %d seconds (and until %s)", pollInterval,
               new Date(System.currentTimeMillis() + 1000
                                                     * pollInterval)));
    try {
      Thread.sleep(1000 * pollInterval);
    } catch (InterruptedException e) {
    }
  }

  /**
   * Prior to Launching a New Environment, lets look and copy the most we can
   *
   * @param curEnv current environment
   */
  private void copyOptionSettings(EnvironmentDescription curEnv) throws Exception {
    /**
     * Skip if we don't have anything
     */
    if (null != this.optionSettings && this.optionSettings.length > 0) {
      return;
    }

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

      boolean bInvalid = harmfulOptionSettingP(curEnv.getEnvironmentId(),
                                               curOptionSetting);

      if (bInvalid) {
        getLog().info(
            format("Excluding Option Setting: %s:%s['%s']",
                   curOptionSetting.getNamespace(),
                   curOptionSetting.getOptionName(),
                   CredentialsUtil.redact(curOptionSetting
                                              .getValue())));
        listIterator.remove();
      } else {
        getLog().info(
            format("Including Option Setting: %s:%s['%s']",
                   curOptionSetting.getNamespace(),
                   curOptionSetting.getOptionName(),
                   CredentialsUtil.redact(curOptionSetting
                                              .getValue())));
      }
    }

    Object __secGroups = project.getProperties().get(
        "beanstalk.securityGroups");

    if (null != __secGroups) {
      String securityGroups = StringUtils.defaultString(__secGroups.toString());

      if (!StringUtils.isBlank(securityGroups)) {
        ConfigurationOptionSetting newOptionSetting = new ConfigurationOptionSetting(
            "aws:autoscaling:launchconfiguration",
            "SecurityGroups", securityGroups);
        newOptionSettings.add(newOptionSetting);
        getLog().info(
            format("Including Option Setting: %s:%s['%s']",
                   newOptionSetting.getNamespace(),
                   newOptionSetting.getOptionName(),
                   newOptionSetting.getValue()));
      }
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
   *  @param newEnvironmentId environment id
   * @param curEnvironmentId environment id
   * @param newEnv
   */
  protected void swapEnvironmentCNames(String newEnvironmentId,
                                       String curEnvironmentId, String cnamePrefix,
                                       EnvironmentDescription newEnv)
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

    /*
     * Changes in Route53 as well.
     */
    if (null != domains) {
      List<String> domainsToUse = new ArrayList<String>();

      for (String s : domains) {
        if (isNotBlank(s)) {
          domainsToUse.add(s.trim());
        }
      }

      if (!domainsToUse.isEmpty()) {

        final BindDomainsContext
            ctx =
            new BindDomainsContextBuilder().withCurEnv(newEnv).withDomains(domainsToUse)
                .build();

        new BindDomainsCommand(this).execute(
            ctx);
      } else {
        getLog().info("Skipping r53 domain binding");
      }

    }

    {
      WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
          .withApplicationName(applicationName)//
          .withStatusToWaitFor("Ready")//
          .withEnvironmentRef(newEnvironmentId)//
          .withTimeoutMins(timeoutMins)//
          .build();

      WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(
          this);

      command.execute(context);
    }
  }

  /**
   * Terminates and waits for an environment
   *
   * @param environmentId environment id to terminate
   */
  protected void terminateEnvironment(String environmentId)
      throws AbstractMojoExecutionException {
    if (mockTerminateEnvironment) {
      getLog().info(
          format(
              "We're ignoring the termination of environment id '%s' (see mockTerminateEnvironment)",
              environmentId));

      return;
    }

    Exception lastException = null;
    for (int i = 1; i <= maxAttempts; i++) {
      getLog().info(
          format("Terminating environmentId=%s (attempt %d/%d)",
                 environmentId, i, maxAttempts));

      try {
        TerminateEnvironmentContext terminatecontext = new TerminateEnvironmentContextBuilder()
            .withEnvironmentId(environmentId)
            .withTerminateResources(true).build();
        TerminateEnvironmentCommand command = new TerminateEnvironmentCommand(
            this);

        command.execute(terminatecontext);

        return;
      } catch (Exception exc) {
        lastException = exc;
      }
    }

    throw new MojoFailureException("Unable to terminate environment "
                                   + environmentId, lastException);
  }

  /**
   * Waits for an environment to get ready. Throws an exception either if this environment couldn't
   * get into Ready state or there was a timeout
   *
   * @param environmentId environmentId to wait for
   * @return EnvironmentDescription in Ready state
   */
  protected EnvironmentDescription waitForEnvironment(String environmentId)
      throws AbstractMojoExecutionException {
    getLog().info(
        "Waiting for environmentId " + environmentId
        + " to get into Ready state");

    WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
        .withApplicationName(applicationName)
        .withStatusToWaitFor("Ready").withEnvironmentRef(environmentId)
        .withHealth("Green")
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

    while (hasEnvironmentFor(applicationName, cnamePrefixToReturn) || isNamedEnvironmentUnavailable(
        cnamePrefixToReturn)) {
      cnamePrefixToReturn = String.format("%s-%d", cnamePrefix, i++);
    }

    return cnamePrefixToReturn;
  }

  /**
   * Boolean predicate for environment existence
   *
   * @param applicationName application name
   * @param cnamePrefix     cname prefix
   * @return true if the application name has this cname prefix
   */
  protected boolean hasEnvironmentFor(String applicationName,
                                      String cnamePrefix) {
    return null != getEnvironmentFor(applicationName, cnamePrefix);
  }

  /**
   * Returns the environment description matching applicationName and environmentRef
   *
   * @param applicationName application name
   * @param cnamePrefix     cname prefix
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
    for (EnvironmentDescription envDesc : environments) {
      if (envDesc.getCNAME().equals(cnameToMatch)) {
        return envDesc;
      }
    }

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

    while (containsNamedEnvironment(result) || isNamedEnvironmentUnavailable(result)) {
      result = formatAndTruncate("%s-%d", MAX_ENVNAME_LEN,
                                 environmentRadical, i++);
    }

    return result;
  }

  private boolean isNamedEnvironmentUnavailable(String cnamePrefix) {
    return !getService().checkDNSAvailability(new CheckDNSAvailabilityRequest(cnamePrefix))
        .isAvailable();
  }

  /**
   * Elastic Beanstalk Contains a Max EnvironmentName Limit. Lets truncate it, shall we?
   *
   * @param mask   String.format Mask
   * @param maxLen Maximum Length
   * @param args   String.format args
   * @return formatted String, or maxLen rightmost characters
   */
  protected String formatAndTruncate(String mask, int maxLen, Object... args) {
    String result = String.format(mask, args);

    if (result.length() > maxLen) {
      result = result
          .substring(result.length() - maxLen, result.length());
    }

    return result;
  }

  /**
   * Boolean predicate for named environment
   *
   * @param environmentName environment name
   * @return true if environment name exists
   */
  protected boolean containsNamedEnvironment(String environmentName) {
    for (EnvironmentDescription envDesc : getEnvironmentsFor(applicationName)) {
      if (envDesc.getEnvironmentName().equals(environmentName)) {
        return true;
      }
    }

    return false;
  }
}

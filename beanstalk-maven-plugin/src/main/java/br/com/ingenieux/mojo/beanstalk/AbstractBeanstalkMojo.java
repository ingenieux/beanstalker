/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ingenieux.mojo.beanstalk;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification;
import com.amazonaws.services.elasticbeanstalk.model.SolutionStackDescription;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;
import br.com.ingenieux.mojo.aws.util.GlobUtil;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.util.ConfigUtil;
import br.com.ingenieux.mojo.beanstalk.util.EnvironmentHostnameUtil;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class AbstractBeanstalkMojo extends AbstractAWSMojo<AWSElasticBeanstalkClient> {

  protected List<ConfigurationOptionSetting> getOptionSettings(ConfigurationOptionSetting[] optionSettings) {
    ConfigurationOptionSetting[] arrOptionSettings = optionSettings;

    if (null == arrOptionSettings || 0 == arrOptionSettings.length) {
      return Collections.emptyList();
    }

    return Arrays.asList(arrOptionSettings);
  }

  protected List<OptionSpecification> getOptionsToRemove(OptionSpecification[] optionSettings) {
    OptionSpecification[] arrOptionSettings = optionSettings;

    if (null == arrOptionSettings || 0 == arrOptionSettings.length) {
      return Collections.emptyList();
    }

    return Arrays.asList(arrOptionSettings);
  }

  protected EnvironmentDescription lookupEnvironment(String applicationName, String environmentRef) throws MojoExecutionException {
    final WaitForEnvironmentContext ctx =
        new WaitForEnvironmentContextBuilder().withApplicationName(applicationName).withEnvironmentRef(environmentRef).build();
    final Collection<EnvironmentDescription> environments = new WaitForEnvironmentCommand(this).lookupInternal(ctx);
    return handleResults(environments);
  }

  protected EnvironmentDescription handleResults(Collection<EnvironmentDescription> environments) throws MojoExecutionException {
    int len = environments.size();

    if (1 == len) {
      return environments.iterator().next();
    }

    handleNonSingle(len);

    return null;
  }

  protected void handleNonSingle(int len) throws MojoExecutionException {
    if (0 == len) {
      throw new MojoExecutionException("No environments found");
    } else {
      throw new MojoExecutionException("Multiple environments found matching the supplied parameters (may you file a bug report?)");
    }
  }

  /**
   * Boolean predicate for harmful/placebo options <p/> I really mean harmful - If you mention a
   * terminated environment settings, Elastic Beanstalk will accept, but this might lead to
   * inconsistent states, specially when creating / listing environments. <p/> Trust me on this
   * one.
   *
   * @param environmentId environment id to lookup
   * @param optionSetting option setting
   * @return true if this is not needed
   */
  protected boolean harmfulOptionSettingP(final String environmentId, ConfigurationOptionSetting optionSetting) throws Exception {
    //aws:autoscaling:launchconfiguration:SecurityGroups['sg-18585f7d']
    if (ConfigUtil.optionSettingMatchesP(optionSetting, "aws:autoscaling:launchconfiguration", "SecurityGroups")) {
      final String securityGroup = optionSetting.getValue();

      if (-1 != securityGroup.indexOf(environmentId)) {
        return true;
      }

      if (getLog().isInfoEnabled()) {
        getLog().info("Probing security group '" + securityGroup + "'");
      }

      Validate.isTrue(securityGroup.matches("^sg-\\p{XDigit}{8,17}(,sg-\\p{XDigit}{8,17})*$"), "Invalid Security Group Spec: " + securityGroup);

      final AmazonEC2 ec2 = this.getClientFactory().getService(AmazonEC2Client.class);

      final DescribeSecurityGroupsResult describeSecurityGroupsResult =
          ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupIds(securityGroup));

      if (!describeSecurityGroupsResult.getSecurityGroups().isEmpty()) {
        final Predicate<SecurityGroup> predicate =
            new Predicate<SecurityGroup>() {
              @Override
              public boolean apply(SecurityGroup input) {
                return -1 == input.getGroupName().indexOf(environmentId);
              }
            };

        return Collections2.filter(describeSecurityGroupsResult.getSecurityGroups(), predicate).isEmpty();
      }
    }

    boolean bInvalid = isBlank(optionSetting.getValue());

    if (!bInvalid) {
      bInvalid = (optionSetting.getNamespace().equals("aws:cloudformation:template:parameter") && optionSetting.getOptionName().equals("AppSource"));
    }

    if (!bInvalid) {
      bInvalid = (optionSetting.getNamespace().equals("aws:elasticbeanstalk:sns:topics") && optionSetting.getOptionName().equals("Notification Topic ARN"));
    }

    /*
     * TODO: Apply a more general regex instead
     */
    if (!bInvalid && isNotBlank(environmentId)) {
      bInvalid = (optionSetting.getValue().contains(environmentId));
    }

    return bInvalid;
  }

  public String lookupTemplateName(String applicationName, String templateName) {
    if (!GlobUtil.hasWildcards(defaultString(templateName))) {
      return templateName;
    }

    getLog().info(format("Template Name %s contains wildcards. A Lookup is needed", templateName));

    Collection<String> configurationTemplates = getConfigurationTemplates(applicationName);

    for (String configTemplateName : configurationTemplates) {
      getLog().debug(format(" * Found Template Name: %s", configTemplateName));
    }

    /*
     * TODO: Research and Review valid characters / applicable glob
     * replacements
     */
    Pattern templateMask = GlobUtil.globify(templateName);

    for (String s : configurationTemplates) {
      Matcher m = templateMask.matcher(s);
      if (m.matches()) {
        getLog().info(format("Selecting: %s", s));
        return s;
      }
    }

    getLog().info("Not found");

    return null;
  }

  @SuppressWarnings("unchecked")
  protected List<String> getConfigurationTemplates(String applicationName) {
    List<String> configurationTemplates =
        getService()
            .describeApplications(new DescribeApplicationsRequest().withApplicationNames(applicationName))
            .getApplications()
            .get(0)
            .getConfigurationTemplates();

    Collections.<String>sort(configurationTemplates, new ReverseComparator(String.CASE_INSENSITIVE_ORDER));

    return configurationTemplates;
  }

  /* TODO: Revise Suffix Dynamics */
  public String ensureSuffixStripped(String cnamePrefix) {
    return EnvironmentHostnameUtil.ensureSuffixStripped(cnamePrefix);
  }

  // TODO: Refactor w/ version lookup
  @SuppressWarnings("unchecked")
  protected String lookupSolutionStack(final String solutionStack) {
    if (!GlobUtil.hasWildcards(solutionStack)) {
      return solutionStack;
    }

    getLog().info("Looking up for solution stacks matching '" + solutionStack + "'");

    final Function<SolutionStackDescription, String> stackTransformer =
        new Function<SolutionStackDescription, String>() {
          @Override
          public String apply(SolutionStackDescription input) {
            return input.getSolutionStackName();
          }
        };
    final List<SolutionStackDescription> stackDetails = getService().listAvailableSolutionStacks().getSolutionStackDetails();

    Collection<String> solStackList = Collections2.transform(stackDetails, stackTransformer);

    final Pattern stackPattern = GlobUtil.globify(solutionStack);

    List<String> matchingStacks =
        new ArrayList<String>(
            Collections2.filter(
                solStackList,
                new Predicate<String>() {
                  @Override
                  public boolean apply(String input) {
                    return stackPattern.matcher(input).matches();
                  }
                }));

    Collections.sort(matchingStacks, ComparatorUtils.reversedComparator(Collator.getInstance()));

    if (matchingStacks.isEmpty()) {
      throw new IllegalStateException("unable to lookup a solution stack matching '" + solutionStack + "'");
    }

    return matchingStacks.iterator().next();
  }

  /**
   * Endpoint URL
   */
  @Parameter(property = "beanstalk.endpointUrl")
  protected String endpointUrl;

  @Override
  public AWSElasticBeanstalkClient getService() {
    final AWSElasticBeanstalkClient service = super.getService();

    if (isNotBlank(endpointUrl)) {
      service.setEndpoint(endpointUrl);
    }

    return service;
  }
}

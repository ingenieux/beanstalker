package br.com.ingenieux.mojo.beanstalk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

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

public abstract class AbstractNeedsEnvironmentMojo extends
		AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 **/
	@Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}", required = true)
	protected String applicationName;

	/**
	 * Maven Project
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	/**
	 * cnamePrefix
	 **/
	@Parameter(property = "beanstalk.cnamePrefix")
	protected String cnamePrefix;

	/**
	 * Current Environment
	 */
	protected EnvironmentDescription curEnv;

	@Override
	protected void configure() {
		try {
			curEnv = super.lookupEnvironment(applicationName, cnamePrefix);
		} catch (MojoExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a list of environments for current application name
	 * 
	 * @param cnamePrefix
	 *            cname prefix to match
	 * @return found environment, if any. null otherwise
	 */
	protected EnvironmentDescription getEnvironmentForCNamePrefix(
			String applicationName, String cnamePrefix) {
		for (final EnvironmentDescription env : getEnvironmentsFor(applicationName)) {
			final String cnameToMatch = cnamePrefix + ".elasticbeanstalk.com";
			if (verbose)
				getLog().info(
						"Trying to match " + cnameToMatch + " with "
								+ env.getCNAME());

			if (env.getCNAME().equalsIgnoreCase(cnameToMatch))
				return env;
		}

		return null;
	}

	/**
	 * Returns a list of environments for applicationName
	 * 
	 * @param applicationName
	 *            applicationName
	 * @return environments
	 */
	protected Collection<EnvironmentDescription> getEnvironmentsFor(
			String applicationName) {
		/*
		 * Requests
		 */
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
				.withApplicationName(applicationName).withIncludeDeleted(false);

		return getService().describeEnvironments(req).getEnvironments();
	}

	public static final Comparator<ConfigurationOptionSetting> COS_COMPARATOR = new Comparator<ConfigurationOptionSetting>() {
		@Override
		public int compare(ConfigurationOptionSetting o1,
				ConfigurationOptionSetting o2) {
			return new CompareToBuilder()
					.append(o1.getNamespace(), o2.getNamespace())
					.append(o1.getValue(), o2.getValue()).toComparison();
		}
	};

	protected ConfigurationOptionSetting[] introspectOptionSettings() {
		Set<ConfigurationOptionSetting> configOptionSetting = new TreeSet<ConfigurationOptionSetting>(
				COS_COMPARATOR);

		Properties properties = new Properties();

		if (null != project)
			for (Map.Entry<Object, Object> entry : project.getProperties()
					.entrySet())
				if (("" + entry.getKey()).startsWith("beanstalk"))
					properties.put(entry.getKey(), entry.getValue());

		for (Map.Entry<Object, Object> entry : System.getProperties()
				.entrySet())
			if (("" + entry.getKey()).startsWith("beanstalk"))
				properties.put(entry.getKey(), entry.getValue());

		for (Object o : properties.keySet()) {
			String k = "" + o;

			if (k.startsWith("beanstalk.env.aws.")) {
				String realKey = k.substring("beanstalk.env.".length());
				String v = "" + properties.get(k);
				List<String> elements = new ArrayList<String>(
						Arrays.asList(realKey.split("\\.")));

				String namespace = StringUtils.join(
						elements.subList(0, -1 + elements.size()), ":");
				String optionName = elements.get(-1 + elements.size());

				getLog().info(
						"importing " + k + " as " + namespace + ":"
								+ optionName + "=" + v);

				configOptionSetting.add(new ConfigurationOptionSetting()
						.withNamespace(namespace).withOptionName(optionName)
						.withValue(v));
			} else if (COMMON_PARAMETERS.containsKey(k)) {
				String v = "" + properties.get(k);
				String namespace = COMMON_PARAMETERS.get(k).getNamespace();
				String optionName = COMMON_PARAMETERS.get(k).getOptionName();

				getLog().info(
						"Found alias " + k + " for " + namespace + ":"
								+ optionName + "(value=" + v + ")");

				configOptionSetting.add(new ConfigurationOptionSetting()
						.withNamespace(namespace).withOptionName(optionName)
						.withValue(v));
			}
		}

		if (configOptionSetting.isEmpty())
			return null;

		return (ConfigurationOptionSetting[]) configOptionSetting
				.toArray(new ConfigurationOptionSetting[configOptionSetting
						.size()]);
	}

	protected void waitForNotUpdating()
			throws AbstractMojoExecutionException, MojoFailureException,
			MojoExecutionException {
				WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
						.withApplicationName(applicationName)//
						.withStatusToWaitFor("!Updating")//
						.withEnvironmentId(curEnv.getEnvironmentId())//
						.withTimeoutMins(2)//
						.withDomainToWaitFor(cnamePrefix).build();
			
				WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);
			
				command.execute(context);
			}

	public static final Map<String, ConfigurationOptionSetting> COMMON_PARAMETERS = new TreeMap<String, ConfigurationOptionSetting>() {
		private static final long serialVersionUID = -6380522758234507742L;

		{
			put("beanstalk.keyName", new ConfigurationOptionSetting(
					"aws:autoscaling:launchconfiguration", "EC2KeyName", ""));
			put("beanstalk.applicationHealthCheckURL",
					new ConfigurationOptionSetting(
							"aws:elasticbeanstalk:application",
							"Application Healthcheck URL", ""));
			put("beanstalk.iamInstanceProfile", new ConfigurationOptionSetting(
					"aws:autoscaling:launchconfiguration",
					"IamInstanceProfile", ""));
			put("beanstalk.environmentType", new ConfigurationOptionSetting(
					"aws:elasticbeanstalk:environment", "EnvironmentType", ""));
			put("beanstalk.instanceType", new ConfigurationOptionSetting(
					"aws:autoscaling:launchconfiguration", "InstanceType", ""));
			put("beanstalk.automaticallyTerminateUnhealthyInstances",
					new ConfigurationOptionSetting(
							"aws:elasticbeanstalk:monitoring",
							"Automatically Terminate Unhealthy Instances", ""));
			put("beanstalk.stickinessPolicy", new ConfigurationOptionSetting(
					"aws:elb:policies", "Stickiness Policy", ""));
			put("beanstalk.stickinessCookieExpiration",
					new ConfigurationOptionSetting("aws:elb:policies",
							"Stickiness Cookie Expiration", ""));
			put("beanstalk.availabilityZones", new ConfigurationOptionSetting(
					"aws:autoscaling:asg", "Custom Availability Zones", ""));
			put("beanstalk.notificationProtocol",
					new ConfigurationOptionSetting(
							"aws:elasticbeanstalk:sns:topics",
							"Notification Protocol", ""));
			put("beanstalk.securityGroups",
					new ConfigurationOptionSetting(
							"aws:autoscaling:launchconfiguration",
							"SecurityGroups", ""));
			put("beanstalk.imageId", new ConfigurationOptionSetting(
					"aws:autoscaling:launchconfiguration", "ImageId", ""));
		}
	};
}

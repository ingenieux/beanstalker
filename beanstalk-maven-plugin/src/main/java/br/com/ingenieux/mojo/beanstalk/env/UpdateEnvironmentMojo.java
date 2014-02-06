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

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.update.UpdateEnvironmentContextBuilder;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Updates the environment versionLabel for a given environmentName
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html"
 * >UpdateEnvironment API</a> call.
 * 
 * @since 0.2.0
 */
@Mojo(name = "update-environment")
public class UpdateEnvironmentMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * Version Label to use
	 */
	@Parameter(property = "beanstalk.versionLabel")
	String versionLabel;

	/**
	 * Application Description
	 */
	@Parameter(property = "beanstalk.environmentDescription")
	String environmentDescription;

	/**
	 * <p>
	 * Configuration Option Settings. Will evaluate as such:
	 * </p>
	 * 
	 * <p>
	 * If empty, will lookup for beanstalk.env.aws.x.y variable in the context,
	 * and it will map this variable to namespace [aws.x], under option name y,
	 * unless there's an alias set.
	 * </p>
	 * 
	 * <p>
	 * A Property might be aliased. Current aliases include:
	 * </p>
	 *
     * <ul>
     *   <li>beanstalk.keyName to aws:autoscaling:launchconfiguration/EC2KeyName (EC2 Instance Key)</li>
     *   <li>beanstalk.applicationHealthCheckURL, to aws:elasticbeanstalk:application/Application Healthcheck URL (Application Healthcheck URL)</li>
     *   <li>beanstalk.iamInstanceProfile, to aws:autoscaling:launchconfiguration/IamInstanceProfile (IAM Instance Profile Role Name)</li>
     *   <li>beanstalk.environmentType, to aws:elasticbeanstalk:environment/EnvironmentType (SingleInstance or ELB-bound Environment)</li>
     *   <li>beanstalk.instanceType, to aws:autoscaling:launchconfiguration/InstanceType (EC2 Instance Type to Use)
     *   <li>beanstalk.automaticallyTerminateUnhealthyInstances, to aws:elasticbeanstalk:monitoring/Automatically Terminate Unhealthy Instances (true if should automatically terminate instances)
     *   <li>beanstalk.stickinessPolicy, to aws:elb:policies/Stickiness Policy (ELB Stickiness Policy)</li>
     *   <li>beanstalk.stickinessCookieExpiration, to aws:elb:policies/Stickiness Cookie Expiration" (Stickiness Cookie Expiration Timeout)</li>
     *   <li>beanstalk.availabilityZones, to aws:autoscaling:asg/Custom Availability Zones (Custom AZs to Use)</li>
     *   <li>beanstalk.notificationProtocol, to aws:elasticbeanstalk:sns:topics/Notification Protocol</li>
     *   <li>beanstalk.securityGroups, to aws:autoscaling:launchconfiguration/SecurityGroups</li>
     *   <li>beanstalk.imageId, to aws:autoscaling:launchconfiguration/ImageId</li>
     *   <li>beanstalk.sshSourceRestriction, to aws:autoscaling:launchconfiguration/SSHSourceRestriction</li>
     *   <li>beanstalk.blockDeviceMappings, to aws:autoscaling:launchconfiguration/BlockDeviceMappings</li>
     *   <li>beanstalk.sqsdWorkerQueueURL, to aws:elasticbeanstalk:sqsd/WorkerQueueURL</li>
     *   <li>beanstalk.sqsdHttpPath, to aws:elasticbeanstalk:sqsd/HttpPath</li>
     *   <li>beanstalk.sqsdMimeType, to aws:elasticbeanstalk:sqsd/MimeType</li>
     *   <li>beanstalk.sqsdHttpConnections, to aws:elasticbeanstalk:sqsd/HttpConnections</li>
     *   <li>beanstalk.sqsdConnectTimeout, to aws:elasticbeanstalk:sqsd/ConnectTimeout</li>
     *   <li>beanstalk.sqsdInactivityTimeout, to aws:elasticbeanstalk:sqsd/InactivityTimeout</li>
     *   <li>beanstalk.sqsdVisibilityTimeout, to aws:elasticbeanstalk:sqsd/VisibilityTimeout</li>
     *   <li>beanstalk.sqsdRetentionPeriod, to aws:elasticbeanstalk:sqsd/RetentionPeriod</li>
     * </ul>
	 * 
	 * The reason for most of those aliases if the need to address space and ':'
	 * inside Maven Properties and XML Files.
	 */
	@Parameter
	ConfigurationOptionSetting[] optionSettings;

	/**
	 * <p>
	 * Template Name.
	 * </p>
	 * 
	 * <p>
	 * Could be either literal or a glob, like,
	 * 
	 * <pre>
	 * ingenieux-services-prod-*
	 * </pre>
	 * 
	 * . If a glob, there will be a lookup involved, and the first one in
	 * reverse ASCIIbetical order will be picked upon.
	 * </p>
	 */
	@Parameter(property = "beanstalk.templateName")
	String templateName;

    /**
     * <p>Environment Tier Name (defaults to "WebServer")</p>
     */
    @Parameter(property="beanstalk.environmentTierName", defaultValue="WebServer")
    String environmentTierName;

	protected Object executeInternal() throws AbstractMojoExecutionException {
        versionLabel = lookupVersionLabel(applicationName, versionLabel);

		waitForNotUpdating();
		
		if (null == optionSettings) {
			optionSettings = super.introspectOptionSettings();
		}

		UpdateEnvironmentContext context = UpdateEnvironmentContextBuilder
				.updateEnvironmentContext()
				.withEnvironmentId(curEnv.getEnvironmentId())//
				.withEnvironmentDescription(environmentDescription)//
				.withEnvironmentName(curEnv.getEnvironmentName())//
				.withOptionSettings(optionSettings)//
				.withTemplateName(
						lookupTemplateName(applicationName, templateName))//
				.withVersionLabel(versionLabel)//
				.withLatestVersionLabel(curEnv.getVersionLabel())//
				.build();

		UpdateEnvironmentCommand command = new UpdateEnvironmentCommand(this);

		return command.execute(context);
	}
}

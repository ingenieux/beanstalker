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

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;

import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.create.CreateEnvironmentContextBuilder;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Creates and Launches an Elastic Beanstalk Environment
 *
 * See the docs for the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html"
 * >CreateEnvironment API</a> call.
 *
 * @since 0.1.0
 */
@Mojo(name = "create-environment")
public class CreateEnvironmentMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * environmentName. Takes precedence over environmentRef.
   */
  @Parameter(property = "beanstalk.environmentName", required = true)
  protected String environmentName;
  /**
   * Application Description
   */
  @Parameter(property = "beanstalk.applicationDescription", defaultValue = "${project.name}")
  String applicationDescription;
  /**
   * <p> Configuration Option Settings. Will evaluate as such: </p>
   *
   * <p> If empty, will lookup for beanstalk.env.aws.x.y variable in the context, and it will map
   * this variable to namespace [aws.x], under option name y, unless there's an alias set. </p>
   *
   * <p>A Property might be aliased. Current aliases include:</p>
   *
   * <ul> <li>beanstalk.scalingAvailabilityZones, to aws:autoscaling:asg/Availability Zones</li>
   * <li>beanstalk.scalingCooldown, to aws:autoscaling:asg/Cooldown</li>
   * <li>beanstalk.scalingCustomAvailabilityZones, to aws:autoscaling:asg/Custom Availability
   * Zones</li> <li>beanstalk.scalingMinSize, to aws:autoscaling:asg/MinSize</li>
   * <li>beanstalk.scalingMaxSize, to aws:autoscaling:asg/MaxSize</li>
   *
   * <li>beanstalk.keyName, to aws:autoscaling:launchconfiguration/EC2KeyName (EC2 Instance
   * Key)</li> <li>beanstalk.iamInstanceProfile, to aws:autoscaling:launchconfiguration/IamInstanceProfile
   * (IAM Instance Profile Role Name)</li> <li>beanstalk.imageId, to aws:autoscaling:launchconfiguration/ImageId</li>
   * <li>beanstalk.instanceType, to aws:autoscaling:launchconfiguration/InstanceType (EC2 Instance
   * Type to Use)</li> <li>beanstalk.monitoringInterval, to aws:autoscaling:launchconfiguration/MonitoringInterval</li>
   * <li>beanstalk.securityGroups, to aws:autoscaling:launchconfiguration/SecurityGroups</li>
   * <li>beanstalk.sshSourceRestriction, to aws:autoscaling:launchconfiguration/SSHSourceRestriction</li>
   * <li>beanstalk.blockDeviceMappings, to aws:autoscaling:launchconfiguration/BlockDeviceMappings</li>
   * <li>beanstalk.rootVolumeType, to aws:autoscaling:launchconfiguration/RootVolumeType</li>
   * <li>beanstalk.rootVolumeSize, to aws:autoscaling:launchconfiguration/RootVolumeSize</li>
   * <li>beanstalk.rootVolumeIOPS, to aws:autoscaling:launchconfiguration/RootVolumeIOPS</li>
   *
   * <li>beanstalk.triggerBreachDuration, to aws:autoscaling:trigger/BreachDuration</li>
   * <li>beanstalk.triggerLowerBreachScaleIncrement, to aws:autoscaling:trigger/LowerBreachScaleIncrement</li>
   * <li>beanstalk.triggerLowerThreshold, to aws:autoscaling:trigger/LowerThreshold</li>
   * <li>beanstalk.triggerMeasureName, to aws:autoscaling:trigger/MeasureName</li>
   * <li>beanstalk.triggerPeriod, to aws:autoscaling:trigger/Period</li>
   * <li>beanstalk.triggerStatistic, to aws:autoscaling:trigger/Statistic</li>
   * <li>beanstalk.triggerUnit, to aws:autoscaling:trigger/Unit</li> <li>beanstalk.triggerUpperBreachScaleIncrement,
   * to aws:autoscaling:trigger/UpperBreachScaleIncrement</li> <li>beanstalk.triggerUpperThreshold,
   * to aws:autoscaling:trigger/UpperThreshold</li>
   *
   * <li>beanstalk.rollingupdateMaxBatchSize, to aws:autoscaling:updatepolicy:rollingupdate/MaxBatchSize</li>
   * <li>beanstalk.rollingupdateMinInstancesInService, to aws:autoscaling:updatepolicy:rollingupdate/MinInstancesInService</li>
   * <li>beanstalk.rollingupdatePauseTime, to aws:autoscaling:updatepolicy:rollingupdate/PauseTime</li>
   * <li>beanstalk.rollingupdateEnabled, to aws:autoscaling:updatepolicy:rollingupdate/RollingUpdateEnabled</li>
   *
   * <li>beanstalk.vpcId, to aws:ec2:vpc/VPCId</li> <li>beanstalk.vpcSubnets, to
   * aws:ec2:vpc/Subnets</li> <li>beanstalk.vpcELBSubnets, to aws:ec2:vpc/ELBSubnets</li>
   * <li>beanstalk.vpcELBScheme, to aws:ec2:vpc/ELBScheme</li> <li>beanstalk.vpcDBSubnets, to
   * aws:ec2:vpc/DBSubnets</li> <li>beanstalk.vpcAssociatePublicIpAddress, to
   * aws:ec2:vpc/AssociatePublicIpAddress</li>
   *
   * <li>beanstalk.applicationHealthCheckURL, to aws:elasticbeanstalk:application/Application
   * Healthcheck URL (Application Healthcheck URL)</li>
   *
   * <li>beanstalk.timeout, to aws:elasticbeanstalk:command/Timeout</li>
   *
   * <li>beanstalk.environmentType, to aws:elasticbeanstalk:environment/EnvironmentType
   * (SingleInstance or ELB-bound Environment)</li>
   *
   * <li>beanstalk.automaticallyTerminateUnhealthyInstances, to aws:elasticbeanstalk:monitoring/Automatically
   * Terminate Unhealthy Instances (true if should automatically terminate instances)</li>
   *
   * <li>beanstalk.notificationEndpoint, to aws:elasticbeanstalk:sns:topics/Notification
   * Endpoint</li> <li>beanstalk.notificationProtocol, to aws:elasticbeanstalk:sns:topics/Notification
   * Protocol</li> <li>beanstalk.notificationTopicARN, to aws:elasticbeanstalk:sns:topics/Notification
   * Topic ARN</li> <li>beanstalk.notificationTopicName, to aws:elasticbeanstalk:sns:topics/Notification
   * Topic Name</li>
   *
   * <li>beanstalk.sqsdWorkerQueueUrl, to aws:elasticbeanstalk:sqsd/WorkerQueueURL</li>
   * <li>beanstalk.sqsdHttpPath, to aws:elasticbeanstalk:sqsd/HttpPath</li>
   * <li>beanstalk.sqsdMimeType, to aws:elasticbeanstalk:sqsd/MimeType</li>
   * <li>beanstalk.sqsdHttpConnections, to aws:elasticbeanstalk:sqsd/HttpConnections</li>
   * <li>beanstalk.sqsdConnectTimeout, to aws:elasticbeanstalk:sqsd/ConnectTimeout</li>
   * <li>beanstalk.sqsdInactivityTimeout, to aws:elasticbeanstalk:sqsd/InactivityTimeout</li>
   * <li>beanstalk.sqsdVisibilityTimeout, to aws:elasticbeanstalk:sqsd/VisibilityTimeout</li>
   * <li>beanstalk.sqsdRetentionPeriod, to aws:elasticbeanstalk:sqsd/RetentionPeriod</li>
   * <li>beanstalk.sqsdMaxRetries, to aws:elasticbeanstalk:sqsd/MaxRetries</li>
   *
   * <li>beanstalk.healthcheckHealthyThreshold, to aws:elb:healthcheck/HealthyThreshold</li>
   * <li>beanstalk.healthcheckInterval, to aws:elb:healthcheck/Interval</li>
   * <li>beanstalk.healthcheckTimeout, to aws:elb:healthcheck/Timeout</li>
   * <li>beanstalk.healthcheckUnhealthyThreshold, to aws:elb:healthcheck/UnhealthyThreshold</li>
   *
   * <li>beanstalk.loadBalancerHTTPPort, to aws:elb:loadbalancer/LoadBalancerHTTPPort</li>
   * <li>beanstalk.loadBalancerPortProtocol, to aws:elb:loadbalancer/LoadBalancerPortProtocol</li>
   * <li>beanstalk.loadBalancerHTTPSPort, to aws:elb:loadbalancer/LoadBalancerHTTPSPort</li>
   * <li>beanstalk.loadBalancerSSLPortProtocol, to aws:elb:loadbalancer/LoadBalancerSSLPortProtocol</li>
   * <li>beanstalk.loadBalancerSSLCertificateId, to aws:elb:loadbalancer/SSLCertificateId</li>
   *
   * <li>beanstalk.stickinessCookieExpiration, to aws:elb:policies/Stickiness Cookie Expiration
   * (Stickiness Cookie Expiration Timeout)</li> <li>beanstalk.stickinessPolicy, to
   * aws:elb:policies/Stickiness Policy (ELB Stickiness Policy)</li>
   *
   * <li>beanstalk.dbAllocatedStorage, to aws:rds:dbinstance/DBAllocatedStorage</li>
   * <li>beanstalk.dbDeletionPolicy, to aws:rds:dbinstance/DBDeletionPolicy</li>
   * <li>beanstalk.dbEngine, to aws:rds:dbinstance/DBEngine</li> <li>beanstalk.dbEngineVersion, to
   * aws:rds:dbinstance/DBEngineVersion</li> <li>beanstalk.dbInstanceClass, to
   * aws:rds:dbinstance/DBInstanceClass</li> <li>beanstalk.dbPassword, to
   * aws:rds:dbinstance/DBPassword</li> <li>beanstalk.dbSnapshotIdentifier, to
   * aws:rds:dbinstance/DBSnapshotIdentifier</li> <li>beanstalk.dbUser, to
   * aws:rds:dbinstance/DBUser</li> <li>beanstalk.dbMultiAZDatabase, to
   * aws:rds:dbinstance/MultiAZDatabase</li>
   *
   * <li>beanstalk.environmentAwsSecretKey, to aws:elasticbeanstalk:application:environment/AWS_SECRET_KEY</li>
   * <li>beanstalk.environmentAwsAccessKeyId, to aws:elasticbeanstalk:application:environment/AWS_ACCESS_KEY_ID</li>
   * <li>beanstalk.environmentJdbcConnectionString, to aws:elasticbeanstalk:application:environment/JDBC_CONNECTION_STRING</li>
   * <li>beanstalk.environmentParam1, to aws:elasticbeanstalk:application:environment/PARAM1</li>
   * <li>beanstalk.environmentParam2, to aws:elasticbeanstalk:application:environment/PARAM2</li>
   * <li>beanstalk.environmentParam3, to aws:elasticbeanstalk:application:environment/PARAM3</li>
   * <li>beanstalk.environmentParam4, to aws:elasticbeanstalk:application:environment/PARAM4</li>
   * <li>beanstalk.environmentParam5, to aws:elasticbeanstalk:application:environment/PARAM5</li>
   *
   * <li>beanstalk.logPublicationControl, to aws:elasticbeanstalk:hostmanager/LogPublicationControl</li>
   *
   * <li>beanstalk.jvmOptions, to aws:elasticbeanstalk:container:tomcat:jvmoptions/JVM Options</li>
   * <li>beanstalk.jvmXmx, to aws:elasticbeanstalk:container:tomcat:jvmoptions/Xmx</li>
   * <li>beanstalk.jvmMaxPermSize, to aws:elasticbeanstalk:container:tomcat:jvmoptions/XX:MaxPermSize</li>
   * <li>beanstalk.jvmXms, to aws:elasticbeanstalk:container:tomcat:jvmoptions/Xms</li>
   *
   * <li>beanstalk.phpDocumentRoot, to aws:elasticbeanstalk:container:php:phpini/document_root</li>
   * <li>beanstalk.phpMemoryLimit, to aws:elasticbeanstalk:container:php:phpini/memory_limit</li>
   * <li>beanstalk.phpZlibOutputCompression, to aws:elasticbeanstalk:container:php:phpini/zlib.output_compression</li>
   * <li>beanstalk.phpAllowUrlFopen, to aws:elasticbeanstalk:container:php:phpini/allow_url_fopen</li>
   * <li>beanstalk.phpDisplayErrors, to aws:elasticbeanstalk:container:php:phpini/display_errors</li>
   * <li>beanstalk.phpMaxExecutionTime, to aws:elasticbeanstalk:container:php:phpini/max_execution_time</li>
   * <li>beanstalk.phpComposerOptions, to aws:elasticbeanstalk:container:php:phpini/composer_options</li>
   * </ul>
   *
   * The reason for most of those aliases if the need to address space and ':' inside Maven
   * Properties and XML Files.
   */
  @Parameter
  ConfigurationOptionSetting[] optionSettings;

  /**
   * Version Label to use
   */
  @Parameter(property = "beanstalk.versionLabel")
  String versionLabel;

  /**
   * Solution Stack Name
   */
  @Parameter(property = "beanstalk.solutionStack",
             defaultValue = "32bit Amazon Linux running Tomcat 7")
  String solutionStack;

  /**
   * <p>Template Name.</p>
   *
   * <p>Could be either literal or a glob, like, <pre>ingenieux-services-prod-*</pre>. If a glob,
   * there will be a lookup involved, and the first one in reverse ASCIIbetical order will be picked
   * upon. </p>
   */
  @Parameter(property = "beanstalk.templateName")
  String templateName;

  /**
   * <p>Status to Wait For</p>
   *
   * <p>Optional. If set, will block until app status is set eg "Ready"</p>
   */
  @Parameter(property = "beanstalk.waitForReady", defaultValue = "true")
  boolean waitForReady;

  /**
   * <p>Environment Tier Name (defaults to "WebServer")</p>
   */
  @Parameter(property = "beanstalk.environmentTierName", defaultValue = "WebServer")
  String environmentTierName;

  /**
   * <p>CNAME Prefix</p>
   */
  @Parameter(property = "beanstalk.cnamePrefix")
  String cnamePrefix;

  /**
   * Overrides parent in order to avoid a thrown exception as there's not an environment to lookup
   */
  @Override
  protected void configure() {
    // Disable parent lookup - We're CREATING, mind that!
  }

  @Override
  protected Object executeInternal() throws Exception {
    versionLabel = lookupVersionLabel(applicationName, versionLabel);

    CreateEnvironmentResult result = createEnvironment(cnamePrefix, this.environmentName);

    return result;
  }

  protected CreateEnvironmentResult createEnvironment(String cnameToCreate,
                                                      String newEnvironmentName)
      throws AbstractMojoExecutionException {
                /*
                 * Hey Aldrin, have you ever noticed we're getting pedantic on those validations?
		 */
    Validate.isTrue(isNotBlank(newEnvironmentName), "No New Environment Name Supplied");

    if (null == optionSettings) {
      optionSettings = introspectOptionSettings();
    }

    versionLabel = lookupVersionLabel(applicationName, versionLabel);

    CreateEnvironmentContextBuilder builder = CreateEnvironmentContextBuilder
        .createEnvironmentContext() //
        .withApplicationName(applicationName)//
        .withApplicationDescription(applicationDescription)//
        .withCnamePrefix(cnameToCreate)//
        .withSolutionStack(lookupSolutionStack(solutionStack))//
        .withTemplateName(templateName)//
        .withEnvironmentName(newEnvironmentName)//
        .withOptionSettings(optionSettings)//
        .withEnvironmentTierName(environmentTierName)//
        .withVersionLabel(versionLabel);//

    CreateEnvironmentContext context = builder.build();

    CreateEnvironmentCommand command = new CreateEnvironmentCommand(this);

    CreateEnvironmentResult result = command.execute(context);

    if (waitForReady) {
      WaitForEnvironmentContext ctx = new WaitForEnvironmentContextBuilder()//
          .withEnvironmentRef(result.getEnvironmentId())//
          .withApplicationName(result.getApplicationName())//
          .withHealth("Green")//
          .withStatusToWaitFor("Ready")//
          .build();

      new WaitForEnvironmentCommand(this).execute(ctx);
    }

    return result;
  }

}

package br.com.ingenieux.mojo.beanstalk;

import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;
import com.amazonaws.services.elasticbeanstalk.model.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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

    public static final String BEANSTALKX_PREFIX = "beanstalkx.";

    public static class ConfigurableOptionSetting {
        String alias;

        String namespace;

        String optionName;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getOptionName() {
            return optionName;
        }

        public void setOptionName(String optionName) {
            this.optionName = optionName;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("namespace", namespace).append("optionName", optionName).toString();
        }
    }

    /***
     * <p>Allows you to declare your own properties inside the plugin configuration, as long as it starts with "beanstalk.x."
     * <p/>
     * <p/>
     * <code><pre>
     * &lt;pluginManagement&gt;
     *   &lt;plugins&gt;
     *     &lt;plugin&gt;
     *       &lt;groupId&gt;br.com.ingenieux&lt;/groupId&gt;
     *       &lt;artifactId&gt;beanstalk-maven-plugin&lt;/artifactId&gt;
     *       &lt;inherited&gt;true&lt;/inherited&gt;
     *       &lt;version&gt;XXX&lt;/version&gt;
     *       &lt;configuration&gt;
     *         &lt;tags&gt;
     *           &lt;Uses&gt;ingenieux Beanstalker, http://beanstalker.ingenieux.com.br/&lt;/Uses&gt;
     *         &lt;/tags&gt;
     *         &lt;configurableOptionSettings&gt;
     *           &lt;configurableOptionSetting&gt;
     *             &lt;alias&gt;beanstalkx.serviceRole&lt;/alias&gt;
     *             &lt;namespace&gt;aws:elasticbeanstalk:environment&lt;/namespace&gt;
     *             &lt;optionName&gt;ServiceRole&lt;/optionName&gt;
     *           &lt;/configurableOptionSetting&gt;
     *         &lt;/configurableOptionSettings&gt;
     *       &lt;/configuration&gt;
     *     &lt;/plugin&gt;
     *   &lt;/plugins&gt;
     * &lt;/pluginManagement&gt;
     * </pre>
     * </code>
     *
     * <p>This basically exposes the <code>beanstalkx.serviceRole</code> property as the mentioned Configuration Option Setting</p>
     *
     */
    @Parameter
    List<ConfigurableOptionSetting> configurableOptionSettings = new ArrayList<ConfigurableOptionSetting>();

    public static final Comparator<ConfigurationOptionSetting>
            COS_COMPARATOR =
            new Comparator<ConfigurationOptionSetting>() {
                @Override
                public int compare(ConfigurationOptionSetting o1,
                                   ConfigurationOptionSetting o2) {
                    return new CompareToBuilder()
                            .append(o1.getNamespace(), o2.getNamespace())
                            .append(o1.getOptionName(), o2.getOptionName())
                            .append(o1.getValue(), o2.getValue()).toComparison();
                }
            };


    public static final Map<String, ConfigurationOptionSetting>
            COMMON_PARAMETERS =
            new TreeMap<String, ConfigurationOptionSetting>() {
                private static final long serialVersionUID = -6380522758234507742L;

                {
                    put("beanstalk.scalingAvailabilityZones", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "Availability Zones", ""));
                    put("beanstalk.scalingCooldown", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "Cooldown", ""));
                    put("beanstalk.scalingCustomAvailabilityZones", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "Custom Availability Zones", ""));
                    put("beanstalk.availabilityZones", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "Custom Availability Zones", ""));
                    put("beanstalk.scalingMinSize", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "MinSize", ""));
                    put("beanstalk.scalingMaxSize", new ConfigurationOptionSetting(
                            "aws:autoscaling:asg", "MaxSize", ""));

                    put("beanstalk.keyName", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "EC2KeyName", ""));


                    put("beanstalk.iamInstanceProfile", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "IamInstanceProfile", ""));

                    put("beanstalk.serviceRole", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:environment", "ServiceRole", ""));

                    put("beanstalk.imageId", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "ImageId", ""));
                    put("beanstalk.instanceType", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "InstanceType", ""));
                    put("beanstalk.monitoringInterval", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "MonitoringInterval", ""));
                    put("beanstalk.securityGroups", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "SecurityGroups", ""));
                    put("beanstalk.sshSourceRestriction", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "SSHSourceRestriction", ""));
                    put("beanstalk.blockDeviceMappings", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "BlockDeviceMappings", ""));
                    put("beanstalk.rootVolumeType", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "RootVolumeType", ""));
                    put("beanstalk.rootVolumeSize", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "RootVolumeSize", ""));
                    put("beanstalk.rootVolumeIOPS", new ConfigurationOptionSetting(
                            "aws:autoscaling:launchconfiguration", "RootVolumeIOPS", ""));

                    put("beanstalk.triggerBreachDuration", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "BreachDuration", ""));
                    put("beanstalk.triggerLowerBreachScaleIncrement", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "LowerBreachScaleIncrement", ""));
                    put("beanstalk.triggerLowerThreshold", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "LowerThreshold", ""));
                    put("beanstalk.triggerMeasureName", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "MeasureName", ""));
                    put("beanstalk.triggerPeriod", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "Period", ""));
                    put("beanstalk.triggerStatistic", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "Statistic", ""));
                    put("beanstalk.triggerUnit", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "Unit", ""));
                    put("beanstalk.triggerUpperBreachScaleIncrement", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "UpperBreachScaleIncrement", ""));
                    put("beanstalk.triggerUpperThreshold", new ConfigurationOptionSetting(
                            "aws:autoscaling:trigger", "UpperThreshold", ""));

                    put("beanstalk.rollingupdateMaxBatchSize", new ConfigurationOptionSetting(
                            "aws:autoscaling:updatepolicy:rollingupdate", "MaxBatchSize", ""));
                    put("beanstalk.rollingupdateMinInstancesInService", new ConfigurationOptionSetting(
                            "aws:autoscaling:updatepolicy:rollingupdate", "MinInstancesInService", ""));
                    put("beanstalk.rollingupdatePauseTime", new ConfigurationOptionSetting(
                            "aws:autoscaling:updatepolicy:rollingupdate", "PauseTime", ""));
                    put("beanstalk.rollingupdateEnabled", new ConfigurationOptionSetting(
                            "aws:autoscaling:updatepolicy:rollingupdate", "RollingUpdateEnabled", ""));

                    put("beanstalk.vpcId", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "VPCId", ""));
                    put("beanstalk.vpcSubnets", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "Subnets", ""));
                    put("beanstalk.vpcELBSubnets", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "ELBSubnets", ""));
                    put("beanstalk.vpcELBScheme", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "ELBScheme", ""));
                    put("beanstalk.vpcDBSubnets", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "DBSubnets", ""));
                    put("beanstalk.vpcAssociatePublicIpAddress", new ConfigurationOptionSetting(
                            "aws:ec2:vpc", "AssociatePublicIpAddress", ""));

                    put("beanstalk.applicationHealthCheckURL", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application", "Application Healthcheck URL", ""));

                    put("beanstalk.timeout", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:command", "Timeout", ""));

                    put("beanstalk.environmentType", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:environment", "EnvironmentType", ""));

                    put("beanstalk.automaticallyTerminateUnhealthyInstances", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:monitoring", "Automatically Terminate Unhealthy Instances",
                            ""));

                    put("beanstalk.notificationEndpoint", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sns:topics", "Notification Endpoint", ""));
                    put("beanstalk.notificationProtocol", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sns:topics", "Notification Protocol", ""));
                    put("beanstalk.notificationTopicARN", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sns:topics", "Notification Topic ARN", ""));
                    put("beanstalk.notificationTopicName", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sns:topics", "Notification Topic Name", ""));

                    put("beanstalk.sqsdWorkerQueueUrl", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "WorkerQueueURL", ""));
                    put("beanstalk.sqsdHttpPath", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "HttpPath", ""));
                    put("beanstalk.sqsdMimeType", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "MimeType", ""));
                    put("beanstalk.sqsdHttpConnections", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "HttpConnections", ""));
                    put("beanstalk.sqsdConnectTimeout", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "ConnectTimeout", ""));
                    put("beanstalk.sqsdInactivityTimeout", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "InactivityTimeout", ""));
                    put("beanstalk.sqsdVisibilityTimeout", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "VisibilityTimeout", ""));
                    put("beanstalk.sqsdRetentionPeriod", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "RetentionPeriod", ""));
                    put("beanstalk.sqsdMaxRetries", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:sqsd", "MaxRetries", ""));

                    put("beanstalk.healthcheckHealthyThreshold", new ConfigurationOptionSetting(
                            "aws:elb:healthcheck", "HealthyThreshold", ""));
                    put("beanstalk.healthcheckInterval", new ConfigurationOptionSetting(
                            "aws:elb:healthcheck", "Interval", ""));
                    put("beanstalk.healthcheckTimeout", new ConfigurationOptionSetting(
                            "aws:elb:healthcheck", "Timeout", ""));
                    put("beanstalk.healthcheckUnhealthyThreshold", new ConfigurationOptionSetting(
                            "aws:elb:healthcheck", "UnhealthyThreshold", ""));

                    put("beanstalk.loadBalancerHTTPPort", new ConfigurationOptionSetting(
                            "aws:elb:loadbalancer", "LoadBalancerHTTPPort", ""));
                    put("beanstalk.loadBalancerPortProtocol", new ConfigurationOptionSetting(
                            "aws:elb:loadbalancer", "LoadBalancerPortProtocol", ""));
                    put("beanstalk.loadBalancerHTTPSPort", new ConfigurationOptionSetting(
                            "aws:elb:loadbalancer", "LoadBalancerHTTPSPort", ""));
                    put("beanstalk.loadBalancerSSLPortProtocol", new ConfigurationOptionSetting(
                            "aws:elb:loadbalancer", "LoadBalancerSSLPortProtocol", ""));
                    put("beanstalk.loadBalancerSSLCertificateId", new ConfigurationOptionSetting(
                            "aws:elb:loadbalancer", "SSLCertificateId", ""));

                    put("beanstalk.stickinessCookieExpiration", new ConfigurationOptionSetting(
                            "aws:elb:policies", "Stickiness Cookie Expiration", ""));
                    put("beanstalk.stickinessPolicy", new ConfigurationOptionSetting(
                            "aws:elb:policies", "Stickiness Policy", ""));

                    put("beanstalk.dbAllocatedStorage", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBAllocatedStorage", ""));
                    put("beanstalk.dbDeletionPolicy", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBDeletionPolicy", ""));
                    put("beanstalk.dbEngine", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBEngine", ""));
                    put("beanstalk.dbEngineVersion", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBEngineVersion", ""));
                    put("beanstalk.dbInstanceClass", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBInstanceClass", ""));
                    put("beanstalk.dbPassword", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBPassword", ""));
                    put("beanstalk.dbSnapshotIdentifier", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBSnapshotIdentifier", ""));
                    put("beanstalk.dbUser", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "DBUser", ""));
                    put("beanstalk.dbMultiAZDatabase", new ConfigurationOptionSetting(
                            "aws:rds:dbinstance", "MultiAZDatabase", ""));

                    put("beanstalk.environmentAwsSecretKey", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "AWS_SECRET_KEY", ""));
                    put("beanstalk.environmentAwsAccessKeyId", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "AWS_ACCESS_KEY_ID", ""));
                    put("beanstalk.environmentJdbcConnectionString", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "JDBC_CONNECTION_STRING", ""));
                    put("beanstalk.environmentParam1", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "PARAM1", ""));
                    put("beanstalk.environmentParam2", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "PARAM2", ""));
                    put("beanstalk.environmentParam3", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "PARAM3", ""));
                    put("beanstalk.environmentParam4", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "PARAM4", ""));
                    put("beanstalk.environmentParam5", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:application:environment", "PARAM5", ""));

                    put("beanstalk.logPublicationControl", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:hostmanager", "LogPublicationControl", ""));

                    put("beanstalk.jvmOptions", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:tomcat:jvmoptions", "JVM Options", ""));
                    put("beanstalk.jvmXmx", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:tomcat:jvmoptions", "Xmx", ""));
                    put("beanstalk.jvmMaxPermSize", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:tomcat:jvmoptions", "XX:MaxPermSize", ""));
                    put("beanstalk.jvmXms", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:tomcat:jvmoptions", "Xms", ""));

                    put("beanstalk.phpDocumentRoot", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "document_root", ""));
                    put("beanstalk.phpMemoryLimit", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "memory_limit", ""));
                    put("beanstalk.phpZlibOutputCompression", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "zlib.output_compression", ""));
                    put("beanstalk.phpAllowUrlFopen", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "allow_url_fopen", ""));
                    put("beanstalk.phpDisplayErrors", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "display_errors", ""));
                    put("beanstalk.phpMaxExecutionTime", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "max_execution_time", ""));
                    put("beanstalk.phpComposerOptions", new ConfigurationOptionSetting(
                            "aws:elasticbeanstalk:container:php:phpini", "composer_options", ""));
                }
            };
    /**
     * Beanstalk Application Name
     */
    @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
            required = true)
    protected String applicationName;
    /**
     * Maven Project
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /* TODO: Revise Suffix Dynamics */
    /**
     * Environment Ref
     */
    @Parameter(property = "beanstalk.environmentRef",
            defaultValue = "${project.artifactId}.elasticbeanstalk.com")
    protected String environmentRef;
    /**
     * Current Environment
     */
    protected EnvironmentDescription curEnv;

    @Override
    protected void configure() {
        try {
            curEnv = super.lookupEnvironment(applicationName, environmentRef);
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of environments for applicationName
     *
     * @param applicationName applicationName
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

    protected ConfigurationOptionSetting[] introspectOptionSettings() {
        Set<ConfigurationOptionSetting> configOptionSetting = new TreeSet<ConfigurationOptionSetting>(
                COS_COMPARATOR);

        Map<String, ConfigurableOptionSetting> extraOptionSettings = new LinkedHashMap<String, ConfigurableOptionSetting>();

        for (ConfigurableOptionSetting e : configurableOptionSettings) {
            if (!e.getAlias().startsWith(BEANSTALKX_PREFIX)) {
                getLog().warn(format("Ignoring ConfigurableOptionSetting '%s' ('%s') since it doesn't start with " + BEANSTALKX_PREFIX, e.getAlias(), e));
                continue;
            }

            getLog().info(format("Declaring alias '%s' for '%s'", e.getAlias(), e));

            extraOptionSettings.put(e.getAlias(), e);
        }

        Properties properties = new Properties();

        if (null != project) {
            for (Map.Entry<Object, Object> entry : project.getProperties()
                    .entrySet()) {
                if (("" + entry.getKey()).startsWith("beanstalk")) {
                    properties.put(entry.getKey(), entry.getValue());
                }
            }
        }

        for (Map.Entry<Object, Object> entry : System.getProperties()
                .entrySet()) {
            if (("" + entry.getKey()).startsWith("beanstalk")) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

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
            } else if (extraOptionSettings.containsKey(k)) {
                ConfigurableOptionSetting cos = extraOptionSettings.get(k);
                String v = "" + properties.get(k);

                if (isNotBlank(v)) {
                    getLog().info(format("Assigning alias '%s' to '%s' (value='%s')", k, cos, v));
                    configOptionSetting.add(new ConfigurationOptionSetting(cos.getNamespace(), cos.getOptionName(), v));
                }
            }
        }

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
                .withTimeoutMins(2)//
                .withEnvironmentRef(environmentRef)//
                .build();

        WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);

        command.execute(context);
    }

    protected String lookupVersionLabel(String appName, String versionLabel) {
        if (StringUtils.isBlank(versionLabel)) {
            DescribeApplicationVersionsResult
                    appVersionsResult =
                    getService().describeApplicationVersions(
                            new DescribeApplicationVersionsRequest().withApplicationName(appName));

            List<ApplicationVersionDescription>
                    appVersionList =
                    new ArrayList<ApplicationVersionDescription>(appVersionsResult.getApplicationVersions());

            Collections.sort(appVersionList, new Comparator<ApplicationVersionDescription>() {
                @Override
                public int compare(ApplicationVersionDescription o1,
                                   ApplicationVersionDescription o2) {
                    return new CompareToBuilder().append(o2.getDateUpdated(), o1.getDateUpdated())
                            .append(o2.getDateCreated(), o1.getDateUpdated()).toComparison();
                }
            });

            if (appVersionList.isEmpty()) {
                String message = "No version label supplied **AND** no app versions available.";

                getLog().info(message);

                throw new IllegalStateException(message);
            } else {
                versionLabel = appVersionList.get(0).getVersionLabel();

                getLog().info("Using latest available application version " + versionLabel);
            }
        }

        return versionLabel;
    }
}

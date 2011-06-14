package com.ingenieux.mojo.mapreduce;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;

/**
 * Launches a new Job Flow
 * 
 * @goal run-job-flow
 * @author Aldrin Leal
 */
public class RunJobFlowsMojo extends AbstractMapreduceMojo {
	/**
	 * Job Name
	 * 
	 * @parameter expr ="${mapreduce.jobName}"
	 *            default-value="${project.artifactId}"
	 */
	String jobName;

	/**
	 * Log URI (S3 Bucket Location. Starts with "s3://bucket/[path]")
	 * 
	 * @parameter expr ="${mapreduce.logUri}"
	 */
	String logUri;

	/**
	 * Termination Protected?
	 * 
	 * @parameter expr ="${mapreduce.terminationProtected}" default-value=false
	 */
	Boolean terminationProtected;

	/**
	 * Slave Type
	 * 
	 * @parameter expr ="${mapreduce.slaveType}" default-value="m1.small"
	 */
	String slaveType;

	/**
	 * Master Type
	 * 
	 * @parameter expr ="${mapreduce.masterType}" default-value="m1.small"
	 */
	String masterType;

	/**
	 * Keep Job Flow Alive?
	 * 
	 * @parameter expr ="${mapreduce.keepJobFlowAlive}" default-value=false
	 */
	Boolean keepJobFlowAlive;

	/**
	 * Instance count
	 * 
	 * @parameter expr ="${mapreduce.instances}" default-value=2
	 */
	Integer instances;

	/**
	 * Hadoop Version
	 * 
	 * @parameter expr ="${mapreduce.hadoopVersion}" default-value="0.20"
	 */
	String hadoopVersion;

	/**
	 * EC2 Key name
	 * 
	 * @parameter expr ="${mapreduce.ec2KeyName}"
	 */
	String ec2KeyName;

	/**
	 * Path
	 * 
	 * @parameter
	 * @required
	 */
	String path;

	/**
	 * Jar Arguments
	 * 
	 * @parameter
	 * @required
	 */
	String[] args;

	/**
	 * Hadoop Main Class
	 * 
	 * @parameter expr ="${mapreduce.mainClass}"
	 * @required
	 */
	String mainClass;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		return service.runJobFlow(getRequest());
	}

	private RunJobFlowRequest getRequest() {
		RunJobFlowRequest req = new RunJobFlowRequest();

		req.setLogUri(logUri);
		req.setName(jobName);

		JobFlowInstancesConfig instances = getInstances();
		req.setInstances(instances);

		HadoopJarStepConfig hadoopJarStepConfig = new HadoopJarStepConfig()
		    .withArgs(args).withMainClass(mainClass).withJar(path);

		req.getSteps().add(new StepConfig("custom-jar-exec", hadoopJarStepConfig));

		return req;
	}

	private JobFlowInstancesConfig getInstances() {
		JobFlowInstancesConfig config = new JobFlowInstancesConfig();

		config.setEc2KeyName(ec2KeyName);
		config.setHadoopVersion(hadoopVersion);
		config.setInstanceCount(instances);
		config.setKeepJobFlowAliveWhenNoSteps(keepJobFlowAlive);
		config.setMasterInstanceType(masterType);

		config.setSlaveInstanceType(slaveType);

		config.setTerminationProtected(terminationProtected);

		return config;
	}
}

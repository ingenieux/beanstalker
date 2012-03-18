package br.com.ingenieux.mojo.mapreduce;

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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;

/**
 * Launches a new Job Flow
 * 
 * @author Aldrin Leal
 */
@MojoGoal("run-job-flow")
public class RunJobFlowsMojo extends AbstractMapreduceMojo {
	/**
	 * Job Name
	 */
	@MojoParameter(expression="${mapreduce.jobName}", defaultValue="${project.artifactId}")
	String jobName;

	/**
	 * Log URI (S3 Bucket Location. Starts with "s3://bucket/[path]")
	 */
	@MojoParameter(expression="${mapreduce.logUri}")
	String logUri;

	/**
	 * Termination Protected?
	 */
	@MojoParameter(expression="${mapreduce.terminationProtected}", defaultValue="false")
	Boolean terminationProtected;

	/**
	 * Slave Type
	 */
	@MojoParameter(expression="${mapreduce.slaveType}", defaultValue="m1.small")
	String slaveType;

	/**
	 * Master Type
	 */
	@MojoParameter(expression="${mapreduce.masterType}", defaultValue="m1.small")
	String masterType;

	/**
	 * Keep Job Flow Alive?
	 */
	@MojoParameter(expression="${mapreduce.keepJobFlowAlive}", defaultValue="false")
	Boolean keepJobFlowAlive;

	/**
	 * Instance count
	 */
	@MojoParameter(expression="${mapreduce.instances}", defaultValue="2")
	Integer instances;

	/**
	 * Hadoop Version
	 */
	@MojoParameter(expression="${mapreduce.hadoopVersion}", defaultValue="0.20")
	String hadoopVersion;

	/**
	 * EC2 Key name
	 */
	@MojoParameter(expression="${mapreduce.ec2KeyName}")
	String ec2KeyName;

	/**
	 * Path
	 */
	@MojoParameter(required=true)
	String path;

	/**
	 * Jar Arguments
	 */
	@MojoParameter(required=true)
	String[] args;

	/**
	 * Hadoop Main Class
	 */
	@MojoParameter(expression="${mapreduce.mainClass}", required=true)
	String mainClass;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		return getService().runJobFlow(getRequest());
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

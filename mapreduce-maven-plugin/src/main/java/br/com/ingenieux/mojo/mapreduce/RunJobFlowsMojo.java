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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;

/**
 * Launches a new Job Flow
 * 
 * @author Aldrin Leal
 */
@Mojo(name="run-job-flow")
public class RunJobFlowsMojo extends AbstractMapreduceMojo {
	/**
	 * Job Name
	 */
	@Parameter(property="mapreduce.jobName", defaultValue="${project.artifactId}")
	String jobName;

	/**
	 * Log URI (S3 Bucket Location. Starts with "s3://bucket/[path]")
	 */
	@Parameter(property="mapreduce.logUri")
	String logUri;

	/**
	 * Termination Protected?
	 */
	@Parameter(property="mapreduce.terminationProtected", defaultValue="false")
	Boolean terminationProtected;

	/**
	 * Slave Type
	 */
	@Parameter(property="mapreduce.slaveType", defaultValue="m1.small")
	String slaveType;

	/**
	 * Master Type
	 */
	@Parameter(property="mapreduce.masterType", defaultValue="m1.small")
	String masterType;

	/**
	 * Keep Job Flow Alive?
	 */
	@Parameter(property="mapreduce.keepJobFlowAlive", defaultValue="false")
	Boolean keepJobFlowAlive;

	/**
	 * Instance count
	 */
	@Parameter(property="mapreduce.instances", defaultValue="2")
	Integer instances;

	/**
	 * Hadoop Version
	 */
	@Parameter(property="mapreduce.hadoopVersion", defaultValue="0.20")
	String hadoopVersion;

	/**
	 * EC2 Key name
	 */
	@Parameter(property="mapreduce.ec2KeyName")
	String ec2KeyName;

	/**
	 * Path
	 */
	@Parameter(required=true)
	String path;

	/**
	 * Jar Arguments
	 */
	@Parameter(required=true)
	String[] args;

	/**
	 * Hadoop Main Class
	 */
	@Parameter(property="mapreduce.mainClass", required=true)
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

package br.com.ingenieux.mojo.mapreduce;

import java.util.Iterator;

import org.apache.commons.beanutils.BeanMap;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;

public abstract class AbstractMapreduceMojo extends AbstractMojo {
	/**
	 * AWS Access Key
	 * 
	 * @parameter expression="${aws.accessKey}"
	 * @required
	 */
	String accessKey;

	/**
	 * AWS Credentials
	 */
	AWSCredentials awsCredentials;

	/**
	 * S3 Bucket
	 * 
	 * @parameter expression="${s3Bucket}" default-value="${project.artifactId}"
	 * @required
	 */
	String s3Bucket;

	/**
	 * S3 Key
	 * 
	 * @parameter expression="${project.build.finalName}.${project.packaging}"
	 * @required
	 */
	String s3Key;

	/**
	 * AWS Secret Key
	 * 
	 * @parameter expression="${aws.secretKey}"
	 * @required
	 */
	String secretKey;

	AmazonElasticMapReduceClient service;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		awsCredentials = getAWSCredentials();

		service = new AmazonElasticMapReduceClient(awsCredentials);

		Object result = null;

		try {
			result = executeInternal();

			getLog().info("SUCCESS");
		} catch (Exception e) {
			getLog().warn("FAILURE: ", e);

			if (MojoExecutionException.class.isAssignableFrom(e.getClass())) {
				throw (MojoExecutionException) e;
			} else if (MojoFailureException.class.isAssignableFrom(e.getClass())) {
				throw (MojoFailureException) e;
			} else {
				throw new MojoFailureException("Failed", e);
			}
		}

		displayResults(result);
	}

	void displayResults(Object result) {
		if (null == result)
			return;

		BeanMap beanMap = new BeanMap(result);
		int i = 0;
		for (Iterator<?> itProperty = beanMap.keyIterator(); itProperty.hasNext(); i++) {
			String propertyName = "" + itProperty.next();
			Object propertyValue = beanMap.get(propertyName);

			if ("class".equals(propertyName))
				continue;

			if (null == propertyValue)
				continue;

			Class<?> propertyClass = null;

			try {
				propertyClass = beanMap.getType(propertyName);
			} catch (Exception e) {
				getLog().warn("Failure on property " + propertyName, e);
			}

			if (null == propertyClass) {
				getLog().info(propertyName + ": " + propertyValue);
			} else {
				getLog().info(
				    propertyName + ": " + propertyValue + " [class: "
				        + propertyClass.getSimpleName() + "]");
			}
		}
	}

	protected abstract Object executeInternal() throws MojoExecutionException,
	    MojoFailureException;

	public AWSCredentials getAWSCredentials() {
		return new BasicAWSCredentials(accessKey, secretKey);
	}
}

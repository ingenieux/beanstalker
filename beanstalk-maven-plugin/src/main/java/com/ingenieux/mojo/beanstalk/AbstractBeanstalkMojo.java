package com.ingenieux.mojo.beanstalk;

import java.util.Iterator;

import org.apache.commons.beanutils.BeanMap;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

public abstract class AbstractBeanstalkMojo extends AbstractMojo {
	/**
	 * AWS Access Key
	 * 
	 * @parameter expression="${aws.accessKey}"
	 * @required
	 */
	String accessKey;

	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.name}"
	 */
	String applicationDescription;

	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${project.artifactId}"
	 * @required
	 */
	String applicationName;

	/**
	 * Auto-Create Application? Defaults to true
	 * 
	 * @parameter expression="${beanstalk.autoCreate}
	 */
	boolean autoCreateApplication = true;

	protected AWSCredentials awsCredentials;

	/**
	 * CName Prefix
	 * 
	 * @parameter expression="${project.artifactId}"
	 */
	String cnamePrefix;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentName}"
	 */
	String environmentName;

	/**
	 * Environment Id
	 * 
	 * @parameter expression="${beanstalk.environmentId}"
	 */
	String environmentId;

	/**
	 * Configuration Option Settings
	 * 
	 * @parameter
	 */
	ConfigurationOptionSetting[] optionSettings;

	/**
	 * Options to Remove
	 * 
	 * @parameter
	 */
	OptionToRemove[] optionsToRemove;

	/**
	 * S3 Bucket
	 * 
	 * @parameter expression="${s3Bucket}"
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

	/**
	 * Beanstalk Service Client
	 */
	AWSElasticBeanstalk service;

	/**
	 * Solution Stack Name
	 * 
	 * @parameter expression="${beanstalk.solutionStack}"
	 */
	String solutionStack;

	/**
	 * Template Name
	 * 
	 * @parameter expression="${beanstalk.templateName}"
	 */
	String templateName;

	/**
	 * Auto-Create Application? Defaults to true
	 * 
	 * @parameter expression="${project.version}"
	 */
	String versionLabel;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		awsCredentials = getAWSCredentials();
		service = new AWSElasticBeanstalkClient(awsCredentials);

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

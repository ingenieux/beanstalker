package br.com.ingenieux.mojo.beanstalk;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanMap;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

public abstract class AbstractBeanstalkMojo extends AbstractAWSMojo<AWSElasticBeanstalkClient> {
	/**
	 * AWS Access Key
	 * 
	 * @parameter expression="${aws.accessKey}"
	 */
	private String accessKey;
	
	@Override
	protected String getAccessKey() {
		return accessKey;
	}

	/**
	 * AWS Secret Key
	 * 
	 * @parameter expression="${aws.secretKey}"
	 */
	private String secretKey;
	
	@Override
	protected String getSecretKey() {
		return secretKey;
	}

	@Override
	public final void execute() throws MojoExecutionException,
	    MojoFailureException {
		setupLogging();

		awsCredentials = getAWSCredentials();

		Object result = null;

		try {
			configure();

			result = executeInternal();

			getLog().info("SUCCESS");
		} catch (Exception e) {
			getLog().warn("FAILURE", e);

			handleException(e);
			return;
		}

		displayResults(result);
	}

	/**
	 * Extension Point - Meant for others to declare and redefine variables as
	 * needed.
	 * 
	 */
	protected void configure() {
	}

	public void handleException(Exception e) throws MojoExecutionException,
	    MojoFailureException {
		/*
		 * This is actually the feature I really didn't want to have written, ever.
		 * 
		 * Thank you for reading this comment.
		 */
		if (ignoreExceptions) {
			getLog().warn("Ok. ignoreExceptions is set to true. No result for you!");

			return;
		} else if (MojoExecutionException.class.isAssignableFrom(e.getClass())) {
			throw (MojoExecutionException) e;
		} else if (MojoFailureException.class.isAssignableFrom(e.getClass())) {
			throw (MojoFailureException) e;
		} else {
			throw new MojoFailureException("Failed", e);
		}
	}

	protected void displayResults(Object result) {
		if (null == result)
			return;

		BeanMap beanMap = new BeanMap(result);

		for (Iterator<?> itProperty = beanMap.keyIterator(); itProperty.hasNext();) {
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

	protected abstract Object executeInternal() throws Exception;

	protected List<ConfigurationOptionSetting> getOptionSettings(
	    ConfigurationOptionSetting[] optionSettings) {
		ConfigurationOptionSetting[] arrOptionSettings = optionSettings;

		if (null == arrOptionSettings || 0 == arrOptionSettings.length)
			return Collections.emptyList();

		return Arrays.asList(arrOptionSettings);
	}
}

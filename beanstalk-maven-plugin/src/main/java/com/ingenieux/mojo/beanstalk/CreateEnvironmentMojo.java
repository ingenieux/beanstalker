package com.ingenieux.mojo.beanstalk;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification;

/**
 * Creates and Launches an Elastic Beanstalk Environment
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateEnvironment.html"
 * >CreateEnvironment API</a> call.
 * 
 * @goal create-environment
 */
public class CreateEnvironmentMojo extends AbstractBeanstalkMojo {
	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		CreateEnvironmentRequest request = new CreateEnvironmentRequest();

		request.setApplicationName(applicationName);
		request.setCNAMEPrefix(cnamePrefix);
		request.setDescription(applicationDescription);
		request.setEnvironmentName(environmentName);

		if (null != optionSettings)
			request.setOptionSettings(Arrays.asList(optionSettings));

		request.setOptionsToRemove(getOptionsToRemove());

		request.setSolutionStackName(solutionStack);

		request.setTemplateName(templateName);

		request.setVersionLabel(versionLabel);

		return service.createEnvironment(request);
	}

	private Collection<OptionSpecification> getOptionsToRemove() {
		if (null == this.optionsToRemove)
			return null;

		Collection<OptionSpecification> result = new TreeSet<OptionSpecification>();

		for (OptionToRemove optionToRemove : this.optionsToRemove)
			result.add(optionToRemove);

		return result;
	}
}

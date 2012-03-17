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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDirectInvocation;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

/**
 * Updates the environment configuration (optionsSettings / optionsToRemove)
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateEnvironment.html"
 * >UpdateEnvironment API</a> call.
 * 
 */
@MojoGoal("update-environment-options")
@MojoSince("0.2.2")
@MojoRequiresDirectInvocation
public class UpdateEnvironmentOptionsMojo extends AbstractNeedsEnvironmentMojo {
	public enum WhatToSet {
		description, optionSettings, templateName, versionLabel
	}

	/**
	 * Configuration Option Settings
	 */
	@MojoParameter
	ConfigurationOptionSetting[] optionSettings;

	/**
	 * Environment Name
	 * 
	 * @parameter expression="${beanstalk.environmentDescription}"
	 *            default-value="default"
	 */
	String environmentDescription;

	/**
	 * Version Label to use. Defaults to Project Version
	 */
	@MojoParameter(expression="${beanstalk.versionLabel}", defaultValue="${project.version}")
	String versionLabel;

	/**
	 * Template Name
	 */
	@MojoParameter(expression="${beanstalk.templateName}")
	String templateName;

	/**
	 * What to set?
	 */
	@MojoParameter(expression="${beanstalk.whatToSet}", defaultValue="versionLabel", required=true)
	WhatToSet whatToSet;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		UpdateEnvironmentRequest req = new UpdateEnvironmentRequest();

		req.setEnvironmentId(environmentId);
		req.setEnvironmentName(environmentName);

		if (WhatToSet.versionLabel.equals(whatToSet)) {
			req.setVersionLabel(versionLabel);
		} else if (WhatToSet.description.equals(whatToSet)) {
			req.setDescription(environmentDescription);
		} else if (WhatToSet.optionSettings.equals(whatToSet)) {
			req.setOptionSettings(getOptionSettings(optionSettings));
		} else if (WhatToSet.templateName.equals(whatToSet)) {
			req.setTemplateName(templateName);
//		} else if (WhatToSet.optionsToRemove.equals(whatToSet)) {
//			req.setOptionsToRemove(optionsToRemove)
		}

		return getService().updateEnvironment(req);
	}
}

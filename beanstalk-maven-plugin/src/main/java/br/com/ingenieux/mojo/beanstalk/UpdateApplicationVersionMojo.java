package br.com.ingenieux.mojo.beanstalk;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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

import com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionResult;

/**
 * Updates an Application Version
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_UpdateApplicationVersion.html"
 * >CreateApplicationVersion API</a> call.
 * 
 * @goal update-application-version
 */
public class UpdateApplicationVersionMojo extends AbstractBeanstalkMojo {
	protected Object executeInternal() throws MojoExecutionException {
		UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest();

		request.setApplicationName(applicationName);
		request.setDescription(applicationDescription);
		request.setVersionLabel(versionLabel);

		UpdateApplicationVersionResult result = service
		    .updateApplicationVersion(request);

		return result.getApplicationVersion();
	}
}

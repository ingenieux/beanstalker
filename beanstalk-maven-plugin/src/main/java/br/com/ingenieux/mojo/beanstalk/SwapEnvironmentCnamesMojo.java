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
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;

/**
 * Lists the available solution stacks
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_SwapEnvironmentCNAMEs.html"
 * >SwapEnvironmentCNAMEs API</a> call.
 * 
 * @goal swap-environment-cnames
 * @author Aldrin Leal
 * 
 */
public class SwapEnvironmentCnamesMojo extends AbstractBeanstalkMojo {
	/**
	 * Source Environment Name
	 * 
	 * @parameter expression="${beanstalk.sourceEnvironmentName}"
	 */
	String sourceEnvironmentName;

	/**
	 * Target Environment Name
	 * 
	 * @parameter expression="${beanstalk.targetEnvironmentName}"
	 */
	String targetEnvironmentName;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		SwapEnvironmentCNAMEsRequest req = getRequest();

		if (null != req) {
			service.swapEnvironmentCNAMEs(req);
		} else {
			service.swapEnvironmentCNAMEs();
		}

		return null;
	}

	SwapEnvironmentCNAMEsRequest getRequest() {
		SwapEnvironmentCNAMEsRequest request = new SwapEnvironmentCNAMEsRequest();

		if (StringUtils.isNotBlank(sourceEnvironmentName))
			request.setSourceEnvironmentName(sourceEnvironmentName);

		if (StringUtils.isNotBlank(targetEnvironmentName))
			request.setSourceEnvironmentName(targetEnvironmentName);

		return request;
	}
}

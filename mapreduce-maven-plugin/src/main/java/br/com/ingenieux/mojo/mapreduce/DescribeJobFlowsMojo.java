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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;

/**
 * Describe Flows
 * 
 * @author Aldrin Leal
 * @goal describe-job-flows
 */
public class DescribeJobFlowsMojo extends AbstractMapreduceMojo {
	/**
	 * Comma-Separated List of Job Flow Ids
	 * 
	 * @param expr
	 *          ="${mapreduce.jobFlowIds}"
	 */
	String jobFlowIds;

	@Override
	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		DescribeJobFlowsRequest req = getRequest();

		if (null != req)
			return getService().describeJobFlows(req);

		return getService().describeJobFlows();
	}

	DescribeJobFlowsRequest getRequest() {
		if (StringUtils.isNotBlank(jobFlowIds)) {
			List<String> ids = Arrays.asList(jobFlowIds.split("\\,"));

			return new DescribeJobFlowsRequest(ids);
		}

		return null;
	}
}

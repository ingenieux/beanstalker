package br.com.ingenieux.mojo.mapreduce;

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
			return service.describeJobFlows(req);

		return service.describeJobFlows();
	}

	DescribeJobFlowsRequest getRequest() {
		if (StringUtils.isNotBlank(jobFlowIds)) {
			List<String> ids = Arrays.asList(jobFlowIds.split("\\,"));

			return new DescribeJobFlowsRequest(ids);
		}

		return null;
	}
}

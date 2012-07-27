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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/**
 * Lists the available solution stacks
 * 
 * See the docs for the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_SwapEnvironmentCNAMEs.html"
 * >SwapEnvironmentCNAMEs API</a> call.
 * 
 * @author Aldrin Leal
 * 
 */
@MojoGoal("swap-environment-cnames")
@MojoSince("0.2.3")
public class SwapEnvironmentCnamesMojo extends AbstractBeanstalkMojo {
	/**
	 * Source Environment Name
	 * 
	 */
	@MojoParameter(expression = "${beanstalk.sourceEnvironmentName}")
	String sourceEnvironmentName;

	/**
	 * Source Environment Id
	 */
	@MojoParameter(expression = "${beanstalk.sourceEnvironmentId}")
	String sourceEnvironmentId;

	/**
	 * Destination Environment Name
	 */
	@MojoParameter(expression = "${beanstalk.targetEnvironmentName}")
	String targetEnvironmentName;

	/**
	 * Destination Environment Id
	 */
	@MojoParameter(expression = "${beanstalk.targetEnvironmentId}")
	String targetEnvironmentId;

	/**
	 * Required to specify sourceCname or targetCname
	 */
	@MojoParameter(expression = "${beanstalk.applicationName}", defaultValue = "${project.artifactId}", required = true, description = "Beanstalk Application Name")
	String applicationName;

	/**
	 * Allows specification of the source environment by looking it up by it's
	 * applicationName and CName
	 */
	@MojoParameter(expression = "${beanstalk.sourceEnvironmentCNamePrefix}", description = "CName of source environment")
	String sourceEnvironmentCNamePrefix;

	/**
	 * Allows specification of the target environment by looking it up by it's
	 * applicationName and CName
	 */
	@MojoParameter(expression = "${beanstalk.targetEnvironmentCNamePrefix}", description = "CName of target environment")
	String targetEnvironmentCNamePrefix;

	@Override
	protected Object executeInternal() throws AbstractMojoExecutionException {
		EnvironmentDescription sourceEnvironment = lookupEnvironment("source",
				sourceEnvironmentId, sourceEnvironmentName,
				sourceEnvironmentCNamePrefix);
		EnvironmentDescription targetEnvironment = lookupEnvironment("target",
				targetEnvironmentId, targetEnvironmentName,
				targetEnvironmentCNamePrefix);

		SwapCNamesContext context = SwapCNamesContextBuilder
				.swapCNamesContext()//
				.withSourceEnvironmentId(sourceEnvironment.getEnvironmentId())//
				.withSourceEnvironmentName(
						sourceEnvironment.getEnvironmentName())//
				.withDestinationEnvironmentId(
						targetEnvironment.getEnvironmentId())//
				.withDestinationEnvironmentName(
						targetEnvironment.getEnvironmentName())//
				.build();
		SwapCNamesCommand command = new SwapCNamesCommand(this);

		return command.execute(context);

	}

	protected EnvironmentDescription lookupEnvironment(String kind,
			String environmentId, String environmentName,
			String environmentCNamePrefix) throws MojoExecutionException {
		boolean bIdDefined = isNotBlank(environmentId);
		boolean bNameDefined = isNotBlank(environmentName);
		boolean bCNamePrefixDefined = isNotBlank(environmentCNamePrefix);

		boolean bIdOrNameDefined = bIdDefined ^ bNameDefined;
		
		if (!(bIdOrNameDefined ^ bCNamePrefixDefined)) {
			String message = "You must declare either _EnvironmentId or _EnvironmentName or _EnvironmentCNamePrefix"
					.replaceAll("_", kind);
			throw new MojoExecutionException(message);
		}

		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
				.withApplicationName(applicationName);
		DescribeEnvironmentsResult result = null;

		if (bIdOrNameDefined) {
			if (bIdDefined) {
				req.setEnvironmentIds(Arrays.asList(environmentId));
			} else if (bNameDefined) {
				req.setEnvironmentNames(Arrays.asList(environmentName));
			}

			result = getService().describeEnvironments(req);

			List<EnvironmentDescription> environments = result.getEnvironments();
			
			return handleResults(kind, environments);
		}
		
		result = getService().describeEnvironments(req);
		
		List<EnvironmentDescription> environments = new ArrayList<EnvironmentDescription>();
		
		String cNameToFind = String.format("%s.elasticbeanstalk.com", environmentCNamePrefix);
		
		for (EnvironmentDescription d : result.getEnvironments())
			if (cNameToFind.equals(d.getCNAME()))
				environments.add(d);
		
		handleResults(kind, environments);

		return null;
	}

	private EnvironmentDescription handleResults(String kind,
			List<EnvironmentDescription> environments)
			throws MojoExecutionException {
		int len = environments.size();
		
		if (1 == len)
			return environments.get(0);
		
		handleNonSingle(kind, len);
		
		return null;
	}

	private void handleNonSingle(String kind, int len) throws MojoExecutionException {
		if (0 == len) {
			String message = "No _ environments found matching the supplied parameters"
					.replaceAll("_", kind);

			throw new MojoExecutionException(message);
		} else {
			String message = "Multiple _ environments found matching the supplied parameters (may you file a bug report?)"
					.replaceAll("_", kind);

			throw new MojoExecutionException(message);
		}
	}
}

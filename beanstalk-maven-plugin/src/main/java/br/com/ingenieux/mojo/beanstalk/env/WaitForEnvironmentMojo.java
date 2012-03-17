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
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;

/**
 * Waits for Environment Status to Change
 * 
 */
@MojoGoal("wait-for-environment")
@MojoSince("0.2.2")
public class WaitForEnvironmentMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 */ 
	@MojoParameter(expression="${beanstalk.applicationName}", defaultValue="${project.artifactId}", required=true, description="Beanstalk Application Name")
	String applicationName;

	/**
	 * Minutes until timeout
	 */
	@MojoParameter(expression="${beanstalk.timeoutMins}", defaultValue="20")
	Integer timeoutMins;

	/**
	 * Status to Wait For
	 */
	@MojoParameter(expression="${beanstalk.statusToWaitFor}", defaultValue="Ready")
	String statusToWaitFor;

	/**
	 * DNS CName Prefix
	 */
	@MojoParameter(expression="${beanstalk.cnamePrefix}", defaultValue="${project.artifactId}")
	String cnamePrefix;

	@Override
	protected Object executeInternal() throws Exception {
		WaitForEnvironmentContext context = new WaitForEnvironmentContextBuilder()
		    .withApplicationName(applicationName)//
		    .withStatusToWaitFor(statusToWaitFor)//
		    .withDomainToWaitFor(cnamePrefix)//
		    .withTimeoutMins(timeoutMins)
		    .build();

		WaitForEnvironmentCommand command = new WaitForEnvironmentCommand(this);

		return command.execute(context);
	}
}

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

import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContext;
import br.com.ingenieux.mojo.beanstalk.cmd.env.swap.SwapCNamesContextBuilder;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import java.util.Collection;
import org.apache.maven.plugin.MojoExecutionException;

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
	@MojoParameter(expression="${beanstalk.sourceEnvironmentName}")
	String sourceEnvironmentName;

	/**
	 * Source Environment Id
	 */
	@MojoParameter(expression="${beanstalk.sourceEnvironmentId}")
	String sourceEnvironmentId;

	/**
	 * Destination Environment Name
	 */
	@MojoParameter(expression="${beanstalk.destinationEnvironmentName}")
	String destinationEnvironmentName;

	/**
	 * Destination Environment Id
	 */
	@MojoParameter(expression="${beanstalk.destinationEnvironmentId}")
	String destinationEnvironmentId;
        
        /**
         * Required to specify sourceCname or destinationCname
         */
        @MojoParameter(expression="${beanstalk.applicationName}", defaultValue="${project.artifactId}", required=true, description="Beanstalk Application Name")
	String applicationName;
        
        /**
         * Allows specification of the source environment by looking it up by it's applicationName and cname
         */
        @MojoParameter(expression="${beanstalk.sourceCname}", description="Cname of source environment")
        String sourceCname;
        
        /**
         * Allows specification of the destination environment by looking it up by it's applicationName and cname
         */
        @MojoParameter(expression="${beanstalk.destinationCname}", description="Cname of destination environment")
        String destinationCname;

	@Override
	protected Object executeInternal() throws AbstractMojoExecutionException {
                
                if (sourceCname != null && !sourceCname.trim().isEmpty()) {
                    if (sourceEnvironmentName != null && !sourceEnvironmentName.trim().isEmpty()) {
                        throw new MojoExecutionException("Both {beanstalk.sourceEnvironmentName} and {beanstalk.sourceCname} were specified. Only one or the other may be defined.");
                    }
                    if (sourceEnvironmentId != null && !sourceEnvironmentId.trim().isEmpty()) {
                        throw new MojoExecutionException("Both {beanstalk.sourceEnvironmentId} and {beanstalk.sourceCname} were specified. Only one or the other may be defined.");
                    }
                    final EnvironmentDescription sourceEnv = getEnvironmentFor(applicationName, sourceCname);
                    if (sourceEnv != null) {
                        sourceEnvironmentId = sourceEnv.getEnvironmentId();
                        sourceEnvironmentName = sourceEnv.getEnvironmentName();
                    } else {
                        throw new MojoExecutionException("Unable to find an environment with cname = '" + sourceCname + "' for the application '" + applicationName + "'");
                    }
                }

                if (destinationCname != null && !destinationCname.trim().isEmpty()) {
                    if (destinationEnvironmentName != null && !destinationEnvironmentName.trim().isEmpty()) {
                        throw new MojoExecutionException("Both {beanstalk.destinationEnvironmentName} and {beanstalk.destinationCname} were specified. Only one or the other may be defined.");
                    }
                    if (destinationEnvironmentId != null && !destinationEnvironmentId.trim().isEmpty()) {
                        throw new MojoExecutionException("Both {beanstalk.destinationEnvironmentId} and {beanstalk.destinationCname} were specified. Only one or the other may be defined.");
                    }
                    final EnvironmentDescription destEnv = getEnvironmentFor(applicationName, destinationCname);
                    if (destEnv != null) {
                        destinationEnvironmentId = destEnv.getEnvironmentId();
                        destinationEnvironmentName = destEnv.getEnvironmentName();
                    } else {
                        throw new MojoExecutionException("Unable to find an environment with cname = '" + destinationCname + "' for the application '" + applicationName + "'");
                    }
                }
                                                        
            
		SwapCNamesContext context = SwapCNamesContextBuilder.swapCNamesContext()//
		    .withSourceEnvironmentId(sourceEnvironmentId)//
		    .withSourceEnvironmentName(sourceEnvironmentName)//
		    .withDestinationEnvironmentId(destinationEnvironmentId)//
		    .withDestinationEnvironmentName(destinationEnvironmentName)//
		    .build();
		SwapCNamesCommand command = new SwapCNamesCommand(this);

		return command.execute(context);
                               
	}
        
        protected Collection<EnvironmentDescription> getEnvironmentsFor(
	    String applicationName) {
		/*
		 * Requests
		 */
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest()
		    .withApplicationName(applicationName).withIncludeDeleted(false);

		return getService().describeEnvironments(req).getEnvironments();
	}
        
        /**
	 * Returns the environment description matching applicationName and
	 * cnamePrefix
	 * 
	 * @param applicationName
	 *          application name
	 * @param cnamePrefix
	 *          cname prefix
	 * @return environment description
	 */
	protected EnvironmentDescription getEnvironmentFor(String applicationName,
	    String cnamePrefix) {
		Collection<EnvironmentDescription> environments = getEnvironmentsFor(applicationName);
		String cnameToMatch = String.format("%s.elasticbeanstalk.com", cnamePrefix);

		/*
		 * Finds a matching environment
		 */
		for (EnvironmentDescription envDesc : environments)
			if (envDesc.getCNAME().equals(cnameToMatch))
				return envDesc;

		return null;
	}
}

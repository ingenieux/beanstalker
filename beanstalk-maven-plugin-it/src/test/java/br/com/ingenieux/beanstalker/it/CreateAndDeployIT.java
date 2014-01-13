package br.com.ingenieux.beanstalker.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.maven.shared.invoker.InvocationResult;
import org.junit.Test;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public class CreateAndDeployIT extends BaseBeanstalkIntegrationTest {
	@Test
	public void testAppCreation() throws Exception {
		InvocationResult result = invoke("clean deploy beanstalk:create-environment -Pfast-deploy");
		
		assertThat(result.getExitCode(), is(equalTo(0)));
		
		DescribeEnvironmentsResult envs = getEnvironments();
		
		assertThat(envs.getEnvironments().size(), is(equalTo(1)));
		
		EnvironmentDescription envDesc = envs.getEnvironments().get(0);
		
		assertThat(envDesc.getEnvironmentName(), is(equalTo(r("${beanstalk.project.name}-env"))));
	}

}

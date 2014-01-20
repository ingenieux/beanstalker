package br.com.ingenieux.beanstalker.it;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import org.apache.maven.shared.invoker.InvocationResult;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CreateAndDeployIT extends BaseBeanstalkIntegrationTest {
	@Test
	public void testAppCreation() throws Exception {
        InvocationResult result = null;

        removeFileOrDirectory("src/main/webapp/index.txt");

        result = invoke("clean package");

        assertThat("We wanted the archetype to compile cleanly", result.getExitCode(), is(equalTo(0)));

		result = invoke("deploy beanstalk:wait-for-environment beanstalk:put-environment -Pfast-deploy -DskipTests");
		
		assertThat("The deployment shouldn't be a problem, you know.", result.getExitCode(), is(equalTo(0)));
		
		DescribeEnvironmentsResult envs = getEnvironments();

        assertThat("Well, we wanted one environment", envs.getEnvironments().size(), is(equalTo(1)));
		
		EnvironmentDescription envDesc = envs.getEnvironments().get(0);
		
		assertThat("This environment name wasn't expected. Really", envDesc.getEnvironmentName(), startsWith(r("${beanstalk.project.name}-env")));

        writeIntoFile("src/main/webapp/index.txt", "Hello, World %08X!", System.currentTimeMillis() / 1000);

        result = invoke("package deploy -DskipTests -Pfast-deploy");

        sleep(15);

        envs = getEnvironments();

        envDesc = envs.getEnvironments().get(0);

        assertThat("There should be an environment in 'Updating' state", envDesc.getStatus(), is(equalTo("Updating")));

        result = invoke("beanstalk:wait-for-environment -Dbeanstalk.environmentRef=%s -Dbeanstalk.statusToWaitFor=Ready", envDesc.getEnvironmentId());

        writeIntoFile("src/main/webapp/index.txt", "Hello, World %08X!", System.currentTimeMillis() / 1000);

        result = invoke("package deploy -Pdeploy -DskipTests", envDesc.getCNAME());

        envs = getEnvironments();

        assertThat("Environment Ids must be different", envs.getEnvironments().get(0).getEnvironmentId(), is(not(equalTo(envDesc.getEnvironmentId()))));

        final List<EnvironmentDescription> oldEnvironments = service.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentIds(envDesc.getEnvironmentId())).getEnvironments();

        assertThat("There should be a previous environment", oldEnvironments.size(), equalTo(1));

        assertThat("Previous environment should be in 'Terminated' status", oldEnvironments.get(0).getStatus(), is(equalTo("Terminated")));

        result = invoke("beanstalk:terminate-environment");
    }

    @Test
    public void testWorkerLifecycle() throws Exception {
        InvocationResult result = invoke("clean deploy beanstalk:put-environment -Pfast-deploy,worker -DskipTests");

        assertThat(result.getExitCode(), is(equalTo(0)));

        DescribeEnvironmentsResult envs = getEnvironments();

        assertThat(envs.getEnvironments().size(), is(equalTo(1)));

        EnvironmentDescription envDesc = envs.getEnvironments().get(0);

        assertThat(envDesc.getEnvironmentName(), is(equalTo(r("${beanstalk.project.name}-worker"))));

        result = invoke("beanstalk:terminate-environment -Pworker");
    }
}

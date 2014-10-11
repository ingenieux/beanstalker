package br.com.ingenieux.beanstalker.it;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import org.apache.maven.shared.invoker.InvocationResult;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class CreateAndDeployIT extends BaseBeanstalkIntegrationTest {

  @Test
  public void testAppCreation() throws Exception {
    InvocationResult result = null;

    removeFileOrDirectory("src/main/webapp/index.txt");

    result = invoke("clean package");

    assertThat("We wanted the archetype to compile cleanly", result.getExitCode(), is(equalTo(0)));

    result = invoke("package deploy -Ps3-deploy");

    assertThat("The deployment shouldn't be a problem, you know.", result.getExitCode(),
               is(equalTo(0)));

    DescribeEnvironmentsResult envs = getEnvironments();

    assertThat("Well, we wanted one environment", envs.getEnvironments().size(), is(equalTo(1)));

    EnvironmentDescription envDesc = envs.getEnvironments().get(0);

    assertThat("This environment name wasn't expected. Really", envDesc.getEnvironmentName(),
               startsWith(r("${beanstalk.project.name}-env")));

    writeIntoFile("src/main/webapp/index.txt", "Hello, World %08X!",
                  System.currentTimeMillis() / 1000);

    result =
        invoke(
            "package beanstalk:upload-source-bundle beanstalk:create-application-version beanstalk:replace-environment -Dbeanstalk.mockTerminateEnvironment=true -Ps3-deploy");

    sleep(15);

    envs = getEnvironments();

    assertThat("Well, we wanted two environments", envs.getEnvironments().size(), is(equalTo(2)));

    writeIntoFile("src/main/webapp/index.txt", "Hello, World %08X!",
                  System.currentTimeMillis() / 1000);

    result = invoke("package deploy -Pbluegreen-s3-deploy", envDesc.getCNAME());

    envs = getEnvironments();

    assertThat("Environment Ids must be different",
               envs.getEnvironments().get(0).getEnvironmentId(),
               is(not(equalTo(envDesc.getEnvironmentId()))));

    writeIntoFile("src/main/webapp/index.txt", "Hello, World %08X!",
                  System.currentTimeMillis() / 1000);

    result = invoke("package deploy -Pdeploy", envDesc.getCNAME());

    assertThat("Previous deployment should have worked.", result.getExitCode(), is(equalTo(0)));
  }

  @Test
  public void testWorkerLifecycle() throws Exception {
    InvocationResult
        result =
        invoke("clean deploy beanstalk:put-environment -Pfast-deploy,worker -DskipTests");

    assertThat(result.getExitCode(), is(equalTo(0)));

    DescribeEnvironmentsResult envs = getEnvironments();

    assertThat(envs.getEnvironments().size(), is(equalTo(1)));

    EnvironmentDescription envDesc = envs.getEnvironments().get(0);

    assertThat(envDesc.getEnvironmentName(), is(equalTo(r("${beanstalk.project.name}-worker"))));
  }
}

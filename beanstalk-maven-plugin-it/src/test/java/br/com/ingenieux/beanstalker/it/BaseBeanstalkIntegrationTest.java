package br.com.ingenieux.beanstalker.it;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.Before;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;

public class BaseBeanstalkIntegrationTest {
	protected Properties properties;

	protected Invoker invoker;

	protected StrSubstitutor sub;

	protected File projectDir;

	protected BasicAWSCredentials credsProvider;

	protected AWSElasticBeanstalkClient service;
	
	@Before
	public void setUpProject() throws Exception {
		{
			properties = new Properties(System.getProperties());

			properties.load(getClass().getResourceAsStream("/test.properties"));

			StrLookup lookup = StrLookup.mapLookup(properties);

			sub = new StrSubstitutor(lookup);
		}
		
		credsProvider = new BasicAWSCredentials(properties.getProperty("aws.accessKey"), properties.getProperty("aws.secretKey"));
		service = new AWSElasticBeanstalkClient(credsProvider);

		invoker = new DefaultInvoker();

		projectDir = new File(invoker.getWorkingDirectory(),
				sub.replace("target/${beanstalk.project.name}"));

		if (!projectDir.exists()) {
			File baseDir = new File(invoker.getWorkingDirectory(), "target");

			invoker.execute(new DefaultInvocationRequest()
					.setBaseDirectory(baseDir)
					.setGoals(
							Arrays.asList(sub
									.replace(
											"archetype:generate -DarchetypeVersion=${project.version} -DarchetypeGroupId=br.com.ingenieux -DarchetypeArtifactId=elasticbeanstalk-service-webapp-archetype -DgroupId=br.com.ingenieux -DartifactId=${beanstalk.project.name} -Dversion=0.0.1-SNAPSHOT -Dpackage=br.com.ingenieux.sample")
									.split("\\s+"))));
		}
		invoker.setWorkingDirectory(projectDir);
	}

	public InvocationResult invoke(String command) throws Exception {
		return invoker.execute(new DefaultInvocationRequest().setBaseDirectory(
				projectDir).setGoals(
				Arrays.asList(sub.replace(command).split("\\s+"))));
	}
	
	protected DescribeEnvironmentsResult getEnvironments() {
		return service.describeEnvironments(new DescribeEnvironmentsRequest().withApplicationName(r("${beanstalk.project.name}")).withEnvironmentNames(r("${beanstalk.project.name}-env")));
	}
	
	protected String r(String text) {
		return sub.replace(text);
	}
}

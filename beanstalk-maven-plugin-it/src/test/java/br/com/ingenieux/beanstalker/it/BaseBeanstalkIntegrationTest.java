package br.com.ingenieux.beanstalker.it;

import com.google.inject.Guice;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import br.com.ingenieux.beanstalker.it.di.CoreModule;

public class BaseBeanstalkIntegrationTest {

  @Inject
  protected Properties properties;

  protected Invoker invoker;

  @Inject
  protected StrSubstitutor sub;

  protected File projectDir;

  @Inject
  protected AWSCredentials credsProvider;

  @Inject
  protected AWSElasticBeanstalk service;

  @Before
  public void setUpProject() throws Exception {
    Guice.createInjector(new CoreModule()).injectMembers(this);

    invoker = new DefaultInvoker();

    projectDir = new File(r("${user.dir}/target/${beanstalk.project.name}"));

    if (!projectDir.exists()) {
      File baseDir = projectDir.getParentFile();

      baseDir.mkdirs();

      invoker.execute(new DefaultInvocationRequest()
                          .setBaseDirectory(baseDir)
                          .setGoals(
                              Arrays.asList(r(
                                  "archetype:generate -DarchetypeVersion=${project.version} -DarchetypeGroupId=br.com.ingenieux -DarchetypeArtifactId=elasticbeanstalk-service-webapp-archetype -DgroupId=br.com.ingenieux -DartifactId=${beanstalk.project.name} -Dversion=0.0.1-SNAPSHOT -Dpackage=br.com.ingenieux.sample -DarchetypeCatalog=local")
                                                .split("\\s+"))));
    }
    invoker.setWorkingDirectory(projectDir);
  }

  public InvocationResult invoke(String mask, Object... args) throws Exception {
    String command = String.format(mask, args);
    return invoker.execute(new DefaultInvocationRequest().setBaseDirectory(
        projectDir).setGoals(
        Arrays.asList(sub.replace(command).split("\\s+"))));
  }

  @After
  public void after() throws Exception {
    final List<EnvironmentDescription> environments = getEnvironments().getEnvironments();

    for (EnvironmentDescription ed : environments) {
      try {
        System.err.println("Terminating environment id=" + ed.getEnvironmentId());

        service.terminateEnvironment(
            new TerminateEnvironmentRequest().withEnvironmentId(ed.getEnvironmentId())
                .withTerminateResources(true));
      } catch (Exception exc) {
        exc.printStackTrace();
      }
    }
  }

  protected DescribeEnvironmentsResult getEnvironments() {
    return service.describeEnvironments(
        new DescribeEnvironmentsRequest().withApplicationName(r("${beanstalk.project.name}"))
            .withIncludeDeleted(false));
  }

  protected String r(String text) {
    return sub.replace(text);
  }

  protected void removeFileOrDirectory(String path) {
    try {
      final File file = new File(projectDir, path);

      if (file.isDirectory()) {
        FileUtils.deleteDirectory(file);
      } else {
        FileUtils.deleteQuietly(file);
      }
    } catch (Exception exc) {
      exc.printStackTrace();
    }
  }

  protected void writeIntoFile(String path, String mask, Object... args) {
    FileOutputStream fos = null;
    File outputFile = new File(projectDir, path);

    try {
      fos = new FileOutputStream(outputFile);

      IOUtils.write(String.format(mask, args), fos, "UTF-8");
    } catch (Exception exc) {
      // Ignore. Really.

      exc.printStackTrace();
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  public void sleep(int nSecs) {
    try {
      Thread.sleep(nSecs * 1000);
    } catch (Exception exc) {

    }
  }
}

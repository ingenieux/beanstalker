package br.com.ingenieux.beanstalker.it;

import com.google.common.base.Predicate;
import com.google.inject.Guice;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import javax.inject.Inject;

import br.com.ingenieux.beanstalker.it.di.CoreModule;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

public class ProjectCleaner {

  @Inject
  AWSElasticBeanstalk service;
  AbstractBeanstalkMojo nullMojo = new AbstractBeanstalkMojo() {
    @Override
    public AWSElasticBeanstalkClient getService() {
      return (AWSElasticBeanstalkClient) ProjectCleaner.getInstance().service;
    }

    @Override
    protected Object executeInternal() throws Exception {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
  };
  private Predicate<? super EnvironmentDescription>
      activeEnvironmentPredicate =
      new Predicate<EnvironmentDescription>() {
        @Override
        public boolean apply(EnvironmentDescription t) {
          return !t.getStatus().equals("Terminated");
        }
      };

  private ProjectCleaner() {
    Guice.createInjector(new CoreModule()).injectMembers(this);
  }

  public static ProjectCleaner getInstance() {
    return SingletonHolder.INSTANCE;
  }

  public static void main(String[] args) throws Exception {
    try {
      getInstance().execute();
    } catch (Exception exc) {
      // meh
    }
  }

  public void info(String mask, Object... args) {
    System.err.println(String.format(mask, args));
  }

  public void execute() throws Exception {
    for (ApplicationDescription appDesc : service.describeApplications().getApplications()) {
      if (!appDesc.getApplicationName().startsWith("mbit-")) {
        info("Ignoring application name %s", appDesc.getApplicationName());

        continue;
      } else {
        info("Browsing environments for app name %s", appDesc.getApplicationName());
      }

      info("Deleting application");

      service.deleteApplication(
          new DeleteApplicationRequest().withApplicationName(appDesc.getApplicationName())
              .withTerminateEnvByForce(true));
    }
  }

  public static class SingletonHolder {

    public static final ProjectCleaner INSTANCE = new ProjectCleaner();
  }
}

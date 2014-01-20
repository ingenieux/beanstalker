package br.com.ingenieux.beanstalker.it;

import br.com.ingenieux.beanstalker.it.di.CoreModule;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor.WaitForEnvironmentContextBuilder;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.Guice;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class ProjectCleaner {
    @Inject
    AWSElasticBeanstalk service;

    private Predicate<? super EnvironmentDescription> activeEnvironmentPredicate = new Predicate<EnvironmentDescription>() {
        @Override
        public boolean apply(EnvironmentDescription t) {
            return ! t.getStatus().equals("Terminated");
        }
    };

    public static class SingletonHolder {
        public static final ProjectCleaner INSTANCE = new ProjectCleaner();
    }

    public static ProjectCleaner getInstance() {
        return SingletonHolder.INSTANCE;
    }

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

    public void info(String mask, Object... args) {
        System.err.println(String.format(mask, args));
    }

    public void execute() throws Exception {
        for (ApplicationDescription appDesc : service.describeApplications().getApplications()) {
            if (! appDesc.getApplicationName().startsWith("mbit-")) {
                info("Ignoring application name %s", appDesc.getApplicationName());

                continue;
            } else {
                info("Browsing environments for app name %s", appDesc.getApplicationName());
            }

            final List<EnvironmentDescription> environments = service.describeEnvironments(new DescribeEnvironmentsRequest().withApplicationName(appDesc.getApplicationName())).getEnvironments();

            info("There are %d environments", environments.size());

            Collection<EnvironmentDescription> activeEnvironments = Collections2.filter(environments, activeEnvironmentPredicate);

            for (EnvironmentDescription ed : activeEnvironments) {
                info("Terminate environment %s/%s", ed.getEnvironmentId(), ed.getEnvironmentName());

                service.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentId(ed.getEnvironmentId()).withTerminateResources(true));

                new WaitForEnvironmentCommand(nullMojo).execute(new WaitForEnvironmentContextBuilder().withApplicationName(appDesc.getApplicationName()).withEnvironmentRef(ed.getEnvironmentId()).withStatusToWaitFor("Terminated").build());
            }

            info("Deleting application");

            service.deleteApplication(new DeleteApplicationRequest().withApplicationName(appDesc.getApplicationName()).withTerminateEnvByForce(true));
        }
    }

    private ProjectCleaner() {
        Guice.createInjector(new CoreModule()).injectMembers(this);
    }

	public static void main(String[] args) throws Exception {
        getInstance().execute();
    }
}

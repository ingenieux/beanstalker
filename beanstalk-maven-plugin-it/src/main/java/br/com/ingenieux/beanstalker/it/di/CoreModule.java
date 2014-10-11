package br.com.ingenieux.beanstalker.it.di;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import java.util.Properties;

import javax.inject.Singleton;

public class CoreModule extends AbstractModule {

  @Override
  protected void configure() {
    try {
      configureInternal();
    } catch (Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  private void configureInternal() throws Exception {
    Properties properties = new Properties();

    properties.load(getClass().getResourceAsStream("/test.properties"));

    for (Object o : System.getProperties().keySet()) {
      properties.put("" + o, System.getProperty("" + o));
    }

    bind(Properties.class).toInstance(properties);

    StrSubstitutor sub = new StrSubstitutor(StrLookup.mapLookup(properties));

    bind(StrSubstitutor.class).toInstance(sub);
  }

  @Provides
  @Singleton
  @Inject
  public AWSCredentials getAWSCredentials(Properties properties) {
    return new BasicAWSCredentials(properties.getProperty("aws.accessKey"),
                                   properties.getProperty("aws.secretKey"));
  }

  @Provides
  @Singleton
  @Inject
  public AWSElasticBeanstalk getAWSElasticBeanstalk(AWSCredentials creds) {
    return new AWSElasticBeanstalkClient(creds);
  }
}

package br.com.ingenieux.mojo.beanstalk;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;

import junit.framework.TestCase;

import br.com.ingenieux.mojo.aws.util.TypeUtil;
import br.com.ingenieux.mojo.beanstalk.bundle.CreateStorageLocationMojo;

public class TestTypeUtil extends TestCase {

  public void testTypeUtil() {
    Class<?> c = CreateStorageLocationMojo.class;

    Class<?> serviceClass = TypeUtil.getServiceClass(c);

    assertEquals(serviceClass.getName(), AWSElasticBeanstalkClient.class.getName());

  }

}

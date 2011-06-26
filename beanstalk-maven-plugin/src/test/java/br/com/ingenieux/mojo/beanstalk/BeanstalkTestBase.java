package br.com.ingenieux.mojo.beanstalk;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public abstract class BeanstalkTestBase extends AbstractMojoTestCase {

	public BeanstalkTestBase() {
		super();
	}

	protected File getBasePom() {
    return new File(getBasedir(),
  	    "target/test-classes/br/com/ingenieux/mojo/beanstalk/pom.xml");
  }

}
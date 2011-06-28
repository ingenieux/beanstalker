package br.com.ingenieux.mojo.beanstalk;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class MinimalTestSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite(MinimalTestSuite.class.getName());
		// $JUnit-BEGIN$
		suite.addTestSuite(CheckAvailabilityMojoTest.class);
		// $JUnit-END$
		return suite;
	}

	public void run(TestResult result) {
		suite().run(result);
	}
}

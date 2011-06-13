package com.ingenieux.mojo.beanstalk;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

public class CheckAvailabilityMojoTest extends AbstractMojoTestCase {
	private CheckAvailabilityMojo mojo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		File testPom = new File(getBasedir(),
		    "target/test-classes/com/ingenieux/mojo/beanstalk/pom.xml");

		PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
		    "beanstalk-maven-plugin", testPom);

		CheckAvailabilityMojo mojo = (CheckAvailabilityMojo) configureMojo(
		    new CheckAvailabilityMojo(), pluginConfiguration);

		this.mojo = mojo;

	}

	public void testCheckAvailability() throws Exception {
		setVariableValueToObject(mojo, "cnamePrefix", "bmp-demo-" + System.currentTimeMillis());

		mojo.execute();
	}

	public void testFailWhenExists() throws Exception {
		mojo.failWhenExists = true;
		setVariableValueToObject(mojo, "cnamePrefix", "amazon");

		try {
			mojo.execute();

			Assert.fail("Didn't throw up exception");
		} catch (Exception e) {

		}
	}
}

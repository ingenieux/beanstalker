package com.ingenieux.mojo.beanstalk;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

public class CreateApplicationMojoTest extends AbstractMojoTestCase {
	private CreateApplicationMojo mojo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		File testPom = new File(getBasedir(),
		    "target/test-classes/com/ingenieux/mojo/beanstalk/pom.xml");

		PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
		    "beanstalk-maven-plugin", testPom);

		CreateApplicationMojo mojo = (CreateApplicationMojo) configureMojo(
		    new CreateApplicationMojo(), pluginConfiguration);

		this.mojo = mojo;

		setVariableValueToObject(mojo, "applicationName", "bmp-demo");
	}

	public void testInstantiation() throws Exception {
		assertNotNull(mojo);
	}

	public void testCheckAvailability() throws Exception {
		mojo.execute();
	}
}

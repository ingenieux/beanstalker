package br.com.ingenieux.mojo.beanstalk;

import java.io.File;

import org.codehaus.plexus.configuration.PlexusConfiguration;

public class CreateApplicationMojoTest  extends BeanstalkTestBase {
	private CreateApplicationMojo mojo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		File testPom = super.getBasePom();

		PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
		    "beanstalk-maven-plugin", testPom);

		CreateApplicationMojo mojo = (CreateApplicationMojo) configureMojo(
		    new CreateApplicationMojo(), pluginConfiguration);

		this.mojo = mojo;

		setVariableValueToObject(mojo, "applicationName", "test-bmp-demo-" + System.currentTimeMillis());
	}

	public void testInstantiation() throws Exception {
		assertNotNull(mojo);
	}

	public void testCheckAvailability() throws Exception {
		mojo.execute();
	}
}

package com.ingenieux.mojo.beanstalk;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import com.amazonaws.services.s3.AmazonS3Client;

public class CreateApplicationVersionMojoTest extends AbstractMojoTestCase {
	String s3Bucket = "bmp-demo";

	String s3Key = "bmp-demo.war";

	private CreateApplicationVersionMojo mojo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		File testPom = new File(getBasedir(),
		    "target/test-classes/com/ingenieux/mojo/beanstalk/pom.xml");

		PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
		    "beanstalk-maven-plugin", testPom);

		CreateApplicationVersionMojo mojo = (CreateApplicationVersionMojo) configureMojo(
		    new CreateApplicationVersionMojo(), pluginConfiguration);

		this.mojo = mojo;

		setVariableValueToObject(mojo, "applicationName", "bmp-demo");
		setVariableValueToObject(mojo, "versionLabel", "0.0.1-SNAPSHOT");

		setVariableValueToObject(mojo, "s3Bucket", s3Bucket);
		setVariableValueToObject(mojo, "s3Key", s3Key);
	}

	public void testInstantiation() throws Exception {
		assertNotNull(mojo);
	}

	public void testDefaultCreateVersion() throws Exception {
		mojo.s3Bucket = mojo.s3Key = null;
		
		mojo.versionLabel = "STANDALONE";

		mojo.execute();
	}

	public void testApplicationVersionWithSource() throws Exception {
		File testFile = new File(getBasedir(),
		    "target/test-classes/com/ingenieux/mojo/beanstalk/test.war");

		AmazonS3Client client = new AmazonS3Client(mojo.getAWSCredentials());

		client.createBucket(s3Bucket);

		client.putObject(s3Bucket, s3Key, testFile);
		
		mojo.execute();
	}
}

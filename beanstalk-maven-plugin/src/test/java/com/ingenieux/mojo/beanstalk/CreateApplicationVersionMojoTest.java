package com.ingenieux.mojo.beanstalk;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class CreateApplicationVersionMojoTest extends AbstractMojoTestCase {
	String s3Bucket = "bmp-demo";

	String s3Key = "bmp-demo.war";

	private CreateApplicationVersionMojo mojo;

	private File testFile;

	private AmazonS3Client client;

	private String applicationName;

	private String versionLabel;

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

		mojo.autoCreateApplication = true;

		applicationName = "test-bmp-demo-" + System.currentTimeMillis();
		versionLabel = "0.0.1-SNAPSHOT";

		mojo.s3Bucket = s3Bucket = applicationName;

		setVariableValueToObject(mojo, "applicationName", applicationName);
		setVariableValueToObject(mojo, "versionLabel", versionLabel);

		setVariableValueToObject(mojo, "s3Bucket", s3Bucket);
		setVariableValueToObject(mojo, "s3Key", s3Key);

		client = new AmazonS3Client(mojo.getAWSCredentials());

		if (!client.doesBucketExist(mojo.s3Bucket))
			client.createBucket(s3Bucket);

		this.testFile = new File(getBasedir(), "src/test/resources/com/ingenieux/mojo/beanstalk/test-war.war");

		client.putObject(s3Bucket, s3Key, testFile);
	}

	public void tearDown() {
		if (null != mojo.service) {
			DescribeApplicationVersionsResult describeApplicationVersions = mojo.service
			    .describeApplicationVersions();

			for (ApplicationVersionDescription av : describeApplicationVersions
			    .getApplicationVersions()) {
				if (!av.getApplicationName().startsWith("test-bmp-demo-"))
					continue;

				mojo.service
				    .deleteApplicationVersion(new DeleteApplicationVersionRequest(av
				        .getApplicationName(), av.getVersionLabel()));
			}

			DescribeApplicationsResult describeApplications = mojo.service
			    .describeApplications();

			for (ApplicationDescription ad : describeApplications.getApplications()) {
				mojo.service.deleteApplication(new DeleteApplicationRequest(ad
				    .getApplicationName()));
			}
		}

		for (Bucket b : client.listBuckets()) {
			String name = b.getName();

			if (!name.startsWith("test-bmp-demo-"))
				continue;

			ObjectListing objects = client.listObjects(name);

			for (S3ObjectSummary resource : objects.getObjectSummaries()) {
				try {
					String key = resource.getKey();

					client.deleteObject(new DeleteObjectRequest(name, key));
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}

			client.deleteBucket(name);
		}
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
		mojo.execute();
	}
}

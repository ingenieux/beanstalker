package br.com.ingenieux.mojo.beanstalk;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@RunWith(JUnit38ClassRunner.class)
public class BootstrapTest extends BeanstalkTestBase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		uploadSourceBundleMojo.artifactFile = getWarFile();
		uploadSourceBundleMojo.s3Bucket = getS3Bucket();
		uploadSourceBundleMojo.s3Key = getS3Path();
		
		createAppVersionMojo.versionLabel = this.versionLabel;
	}

	public void testUploadSourceBundle() throws Exception {
		uploadSourceBundleMojo.execute();

		AmazonS3Client s3Client = new AmazonS3Client(this.credentials);

		S3Object object = s3Client.getObject(uploadSourceBundleMojo.s3Bucket, uploadSourceBundleMojo.s3Key);

		assertNotNull(object);
		assertEquals(object.getObjectMetadata().getContentLength(),
		    uploadSourceBundleMojo.artifactFile.length());
	}

	public void testCreateApplicationVersion() throws Exception {
		createAppVersionMojo.execute();
	}
	
	public void testCreateConfigurationTemplate() throws Exception {
		createConfigurationTemplateMojo.execute();
	}

	public void testDeployment() throws Exception {
	}
}

package br.com.ingenieux.mojo.beanstalk;

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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 * 
 * @goal upload-source-bundle
 */
public class UploadSourceBundleMojo extends AbstractBeanstalkMojo {
	/**
	 * S3 Bucket
	 * 
	 * @parameter expression="${s3Bucket}" default-value="${project.artifactId}"
	 * @required
	 */
	String s3Bucket;

	/**
	 * S3 Key
	 * 
	 * @parameter expression="${beanstalk.s3Key}"
	 *            default-value="${project.build.finalName}.${project.packaging}"
	 * @required
	 */
	String s3Key;

	/**
	 * Artifact to Deploy
	 * 
	 * @parameter expression=
	 *            "${project.build.directory}/${project.build.finalName}.${project.packaging}"
	 */
	File artifactFile;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		String path = artifactFile.getPath();

		if (!(path.endsWith(".war") || path.endsWith(".jar"))) {
			getLog().warn("Not a war/jar file. Skipping");

			return null;
		}

		if (!artifactFile.exists())
			throw new MojoFailureException("Artifact File does not exists! (file="
			    + path);

		AmazonS3Client client = new AmazonS3Client(getAWSCredentials());

		getLog().info("Target Path: s3://" + s3Bucket + "/" + s3Key);
		getLog().info("Uploading artifact file: " + path);

		PutObjectResult result = client.putObject(s3Bucket, s3Key, artifactFile);

		getLog().info("Artifact Uploaded");

		return result;
	}
}

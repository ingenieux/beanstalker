package br.com.ingenieux.mojo.beanstalk.bundle;

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
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 */
@MojoGoal("upload-source-bundle")
@MojoSince("0.1.0")
public class UploadSourceBundleMojo extends AbstractBeanstalkMojo {
	/**
	 * S3 Bucket
	 * 
	 */
	@MojoParameter(expression="${beanstalk.s3Bucket}", defaultValue="${project.artifactId}", required=true)
	String s3Bucket;

	/**
	 * S3 Key
	 */
	@MojoParameter(expression="${beanstalk.s3Key}", defaultValue="${project.build.finalName}.${project.packaging}", required=true)
	String s3Key;

	/**
	 * Artifact to Deploy
	 */
	@MojoParameter(expression="${project.build.directory}/${project.build.finalName}.${project.packaging}")
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

		AmazonS3Client client = new AmazonS3Client(getAWSCredentials(), getClientConfiguration());

		getLog().info("Target Path: s3://" + s3Bucket + "/" + s3Key);
		getLog().info("Uploading artifact file: " + path);

		PutObjectResult result = client.putObject(s3Bucket, s3Key, artifactFile);
		
		getLog().info("Artifact Uploaded");

		return result;
	}
}

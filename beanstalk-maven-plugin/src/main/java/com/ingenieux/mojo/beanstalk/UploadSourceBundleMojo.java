package com.ingenieux.mojo.beanstalk;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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
 * Goal which touches a timestamp file.
 * 
 * @goal upload-source-bundle
 */
public class UploadSourceBundleMojo extends AbstractBeanstalkMojo {
	/**
	 * Artifact to Deploy
	 * 
	 * @parameter expression=
	 *            "${project.build.directory}/${project.build.finalName}.${project.packaging}"
	 */
	File artifactFile;

	protected Object executeInternal() throws MojoExecutionException,
	    MojoFailureException {
		if (!artifactFile.getPath().endsWith(".war")) {
			getLog().warn("Not a war file. Skipping");

			return null;
		}

		if (!artifactFile.exists())
			throw new MojoFailureException("Artifact File does not exists! (file="
			    + artifactFile.getPath());

		AmazonS3Client client = new AmazonS3Client(getAWSCredentials());

		getLog().info("Target Path: s3://" + s3Bucket + "/" + s3Key);
		getLog().info("Uploading artifact file: " + artifactFile.getPath());

		PutObjectResult result = client.putObject(s3Bucket, s3Key, artifactFile);

		getLog().info("Artifact Uploaded");

		return result;
	}
}

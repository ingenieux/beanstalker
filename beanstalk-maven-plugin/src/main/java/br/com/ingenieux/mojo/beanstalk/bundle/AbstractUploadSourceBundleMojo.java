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
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Uploads a packed war/zip file to Amazon S3 for further Deployment.
 *
 * @since 0.1.0
 */
public abstract class AbstractUploadSourceBundleMojo extends AbstractBeanstalkMojo {
  /**
   * S3 Bucket
   */
  @Parameter(property = "beanstalk.s3Bucket", defaultValue = "${project.groupId}.${project.artifactId}")
  String s3Bucket;

  /**
   * S3 Key
   */
  @Parameter(property = "beanstalk.s3Key",
             defaultValue = "${project.artifactId}/${project.build.finalName}.${project.packaging}",
             required = true)
  String s3Key;

  /**
   * <p> Should we do a multipart upload? Defaults to false </p> <p> Enable when you want to be
   * charged slightly less :) </p>
   */
  @Parameter(property = "beanstalk.multipartUpload", defaultValue = "false")
  boolean multipartUpload;

  /**
   * Artifact to Deploy
   */
  @Parameter(property = "beanstalk.artifactFile",
             defaultValue="${project.build.directory}/${project.build.finalName}.${project.packaging}",
             required = true)
  File artifactFile;

  /**
   * Silent Upload?
   */
  @Parameter(property = "beanstalk.silentUpload", defaultValue = "false")
  boolean silentUpload = false;

  /**
   * Version Label to use. Defaults to Project Version
   */
  @Parameter(property = "beanstalk.versionLabel", required = true)
  String versionLabel;

  @Parameter(defaultValue = "${project}")
  MavenProject project;

  protected Object executeInternal() throws Exception {
    File toBeUploaded = getSourceBundle();
    String path = toBeUploaded.getPath();

    if (!(path.endsWith(".war") || path.endsWith(".jar") || path.endsWith(".zip"))) {
      getLog().warn("Not a war/jar/zip file. Skipping");

      return null;
    }

    if (!toBeUploaded.exists()) {
      throw new MojoFailureException(
          "Artifact File does not exist! (file=" + path + ")");
    }

    BeanstalkerS3Client client = new BeanstalkerS3Client(getAWSCredentials(),
                                                         getClientConfiguration(), getRegion());

    client.setMultipartUpload(multipartUpload);
    client.setSilentUpload(silentUpload);

    if (StringUtils.isBlank(s3Bucket)) {
      getLog().info("S3 Bucket not defined.");
      s3Bucket = getService().createStorageLocation().getS3Bucket();

      getLog().info("Using defaults, like: " + s3Bucket);

      project.getProperties().put("beanstalk.s3Bucket", s3Bucket);
    }

    getLog().info("Target Path: s3://" + s3Bucket + "/" + s3Key);
    getLog().info("Uploading artifact file: " + path);

    PutObjectResult result = client.putObject(new PutObjectRequest(s3Bucket, s3Key, toBeUploaded));

    getLog().info("Artifact Uploaded");

    project.getProperties().put("beanstalk.s3Key", s3Key);

    return result;
  }

  /**
   * The logic for retrieving source bundle file is different between Java Tomcat Web Applications
   * and Java SE Applications. Subclass should override this method to retrieve the right source
   * bundle file.
   *
   * @return the correct source bundle file.
   * @throws Exception Exception might be thrown when creating source bundle file.
   */
  abstract protected File getSourceBundle() throws Exception;

}

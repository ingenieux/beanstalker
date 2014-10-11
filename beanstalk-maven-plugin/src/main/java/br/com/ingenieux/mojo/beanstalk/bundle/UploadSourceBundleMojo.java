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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 *
 * @since 0.1.0
 */
@Mojo(name = "upload-source-bundle")
public class UploadSourceBundleMojo extends AbstractBeanstalkMojo {

  /**
   * S3 Bucket
   */
  @Parameter(property = "beanstalk.s3Bucket", defaultValue = "${project.artifactId}",
             required = true)
  String s3Bucket;

  /**
   * S3 Key
   */
  @Parameter(property = "beanstalk.s3Key",
             defaultValue = "${project.artifactId}/${project.build.finalName}-${beanstalk.versionLabel}.${project.packaging}",
             required = true)
  String s3Key;

  /**
   * S3 Service Region.
   *
   * <p> See <a href= "http://docs.amazonwebservices.com/general/latest/gr/rande.html#s3_region"
   * >this list</a> for reference. </p>
   */
  @Parameter(property = "beanstalk.s3Region")
  String s3Region;

  /**
   * <p> Should we do a multipart upload? Defaults to true </p> <p> Disable when you want to be
   * charged slightly less :) </p>
   */
  @Parameter(property = "beanstalk.multipartUpload", defaultValue = "true")
  boolean multipartUpload = false;

  /**
   * Artifact to Deploy
   */
  @Parameter(
      defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}")
  File artifactFile;

  /**
   * Silent Upload?
   */
  @Parameter(property = "beanstalk.silentUpload", defaultValue = "false")
  boolean silentUpload = false;

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException, AmazonServiceException,
                                            AmazonClientException, InterruptedException {
    String path = artifactFile.getPath();

    if (!(path.endsWith(".war") || path.endsWith(".jar") || path.endsWith(".zip"))) {
      getLog().warn("Not a war/jar/zip file. Skipping");

      return null;
    }

    if (!artifactFile.exists()) {
      throw new MojoFailureException(
          "Artifact File does not exists! (file=" + path);
    }

    BeanstalkerS3Client client = new BeanstalkerS3Client(getAWSCredentials(),
                                                         getClientConfiguration());

    client.setMultipartUpload(multipartUpload);
    client.setSilentUpload(silentUpload);

    if (StringUtils.isNotBlank(s3Region)) {
      client.setEndpoint(String.format("s3-%s.amazonaws.com", s3Region));
    }

    getLog().info("Target Path: s3://" + s3Bucket + "/" + s3Key);
    getLog().info("Uploading artifact file: " + path);

    PutObjectResult result = client.putObject(new PutObjectRequest(s3Bucket, s3Key, artifactFile));

    getLog().info("Artifact Uploaded");

    return result;
  }
}

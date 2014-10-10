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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Calendar;
import java.util.Date;

import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 *
 * @since 0.2.7
 */
@Mojo(name = "delete-multiparts")
public class DeleteMultipartsMojo extends AbstractBeanstalkMojo {

  /**
   * S3 Bucket
   */
  @Parameter(property = "beanstalk.s3Bucket", defaultValue = "${project.artifactId}",
             required = true)
  String s3Bucket;

  /**
   * S3 Service Region.
   *
   * <p> See <a href= "http://docs.amazonwebservices.com/general/latest/gr/rande.html#s3_region"
   * >this list</a> for reference. </p>
   */
  @Parameter(property = "beanstalk.s3Region")
  String s3Region;

  /**
   * How many delete to delete? Defaults to 365 days
   */
  @Parameter(property = "beanstalk.daysToDelete", defaultValue = "365")
  Integer daysToDelete;

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException, AmazonServiceException,
                                            AmazonClientException, InterruptedException {
    BeanstalkerS3Client client = new BeanstalkerS3Client(getAWSCredentials(),
                                                         getClientConfiguration());

    Calendar c = Calendar.getInstance();

    c.add(Calendar.DAY_OF_YEAR, -daysToDelete);

    Date since = c.getTime();

    client.deleteMultiparts(s3Bucket, since);

    return null;
  }
}

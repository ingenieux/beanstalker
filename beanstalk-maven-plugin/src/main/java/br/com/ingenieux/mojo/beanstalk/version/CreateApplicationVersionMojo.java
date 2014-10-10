package br.com.ingenieux.mojo.beanstalk.version;

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

import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Creates an Application Version, optionally creating the application itself.
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateApplicationVersion.html"
 * >CreateApplicationVersion API</a> call.
 *
 * @since 0.1.0
 */
@Mojo(name = "create-application-version")
public class CreateApplicationVersionMojo extends AbstractBeanstalkMojo {

  /**
   * Beanstalk Application Name
   */
  @Parameter(property = "beanstalk.applicationName", defaultValue = "${project.artifactId}",
             required = true)
  String applicationName;

  /**
   * Version Description
   *
   * Unfortunately, this is incorrectly named. Anyway...
   */
  @Parameter(property = "beanstalk.versionDescription",
             defaultValue = "Update from beanstalk-maven-plugin")
  String versionDescription;

  /**
   * Auto-Create Application? Defaults to true
   */
  @Parameter(property = "beanstalk.autoCreateApplication", defaultValue = "true")
  boolean autoCreateApplication;

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
   * Version Label to use. Defaults to Project Version
   */
  @Parameter(property = "beanstalk.versionLabel", defaultValue = "${project.version}",
             required = true)
  String versionLabel;

  /**
   * Skip when this versionLabel already exists?
   */
  @Parameter(property = "beanstalk.skipExisting", defaultValue = "true")
  boolean skipExisting;

  protected Object executeInternal() throws MojoExecutionException {
    if (skipExisting) {
      if (versionLabelExists()) {
        getLog().info(
            "VersionLabel "
            + versionLabel
            + " already exists. Skipping creation of new application-version");

        return null;
      }
    }

    CreateApplicationVersionRequest request = new CreateApplicationVersionRequest();

    request.setApplicationName(applicationName);
    request.setDescription(versionDescription);
    request.setAutoCreateApplication(autoCreateApplication);

    if (StringUtils.isNotBlank(s3Bucket) && StringUtils.isNotBlank(s3Key)) {
      request.setSourceBundle(new S3Location(s3Bucket, s3Key));
    }

    request.setDescription(versionDescription);

    request.setVersionLabel(versionLabel);

    CreateApplicationVersionResult result = getService()
        .createApplicationVersion(request);

    return result.getApplicationVersion();
  }

  private boolean versionLabelExists() {
                /*
		 * Builds a request for this very specific version label
		 */
    DescribeApplicationVersionsRequest davRequest = new DescribeApplicationVersionsRequest()
        .withApplicationName(applicationName).withVersionLabels(
            versionLabel);

		/*
		 * Sends the request
		 */
    DescribeApplicationVersionsResult result = getService()
        .describeApplicationVersions(davRequest);

		/*
		 * Non-empty means the application version label *DOES* exist.
		 */
    return !result.getApplicationVersions().isEmpty();
  }
}

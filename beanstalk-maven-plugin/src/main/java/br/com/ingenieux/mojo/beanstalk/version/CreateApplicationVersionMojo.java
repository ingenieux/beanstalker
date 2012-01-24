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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;

/**
 * Creates an Application Version, optionally creating the application itself.
 * 
 * See the <a href=
 * "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CreateApplicationVersion.html"
 * >CreateApplicationVersion API</a> call.
 * 
 * @since 0.1.0
 * @goal create-application-version
 */
public class CreateApplicationVersionMojo extends AbstractBeanstalkMojo {
	/**
	 * Beanstalk Application Name
	 * 
	 * @parameter expression="${beanstalk.applicationName}"
	 *            default-value="${project.artifactId}"
	 * @required
	 */
	String applicationName;

	/**
	 * Application Description
	 * 
	 * @parameter expression="${beanstalk.applicationDescription}"
	 *            default-value="${project.name}"
	 */
	String applicationDescription;

	/**
	 * Auto-Create Application? Defaults to true
	 * 
	 * @parameter expression="${beanstalk.autoCreate}" default-value=true
	 */
	boolean autoCreateApplication;

	/**
	 * S3 Bucket
	 * 
	 * @parameter expression="${beanstalk.s3Bucket}"
	 *            default-value="${project.artifactId}"
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
	 * Version Label to use. Defaults to Project Version
	 * 
	 * @parameter expression="${beanstalk.versionLabel}"
	 *            default-value="${project.version}"
	 * @required
	 */
	String versionLabel;

	/**
	 * Skip when this versionLabel already exists?
	 * 
	 * @parameter expression="${beanstalk.skipExisting}" default-value=true
	 */
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
		request.setDescription(applicationDescription);
		request.setAutoCreateApplication(autoCreateApplication);

		if (StringUtils.isNotBlank(s3Bucket) && StringUtils.isNotBlank(s3Key))
			request.setSourceBundle(new S3Location(s3Bucket, s3Key));

		request.setDescription(applicationDescription);

		request.setVersionLabel(versionLabel);

		CreateApplicationVersionResult result = service
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
		DescribeApplicationVersionsResult result = service
				.describeApplicationVersions(davRequest);

		/*
		 * Non-empty means the application version label *DOES* exist.
		 */
		return !result.getApplicationVersions().isEmpty();
	}
}

package br.com.ingenieux.mojo.cloudfront;

import static org.apache.commons.lang.StringUtils.stripStart;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import javax.activation.MimetypesFileTypeMap;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;
import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;

import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;

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

/**
 * An Abstract Mojo for Cloudfront
 **/
public abstract class AbstractCloudfrontMojo extends
		AbstractAWSMojo<AmazonCloudFrontClient> {
	/**
	 * Declares which distributions this mojo will address.
	 */
	@Parameter
	protected Distribution[] distributions;

	/**
	 * In which directory where to look for resources to upload (s3
	 * distributions) or compare against (custom)?
	 */
	@Parameter(defaultValue= "${project.build.directory}/${project.build.finalName}", required = true)
	protected File webappDirectory;

	protected BeanstalkerS3Client s3Client;

	@Override
	protected void configure() {
		super.configure();

		try {
			this.s3Client = new BeanstalkerS3Client(getAWSCredentials(),
					getClientConfiguration());
			
			/*
			 * While we actually love multipart upload, and are not concerned about billing, we're not playing with cloudfront (yegor, I'm talking to you)
			 */
			this.s3Client.setMultipartUpload(false);
		} catch (MojoFailureException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<String> fetchLocalDistributionFiles(Distribution d)
			throws IOException {
		List<String> filenames = FileUtils.getFileNames(webappDirectory,
				d.includes, d.excludes, false);

		if (filenames.size() > 1000)
			getLog().warn(
					"Wait! We still don't support > 1000 files. **USE AT YOUR OWN PERIL**");

		ListIterator<String> listIterator = filenames.listIterator();
		while (listIterator.hasNext()) {
			String f = listIterator.next();
			String normalized = stripStart(
					FileUtils.normalize(f).replace('\\', '/'), "/");

			listIterator.set(normalized);
		}

		return filenames;
	}

	public static final MimetypesFileTypeMap MIMETYPES_FILES_MAP = new MimetypesFileTypeMap();

	protected String guessMimeType(File file) {
		return MIMETYPES_FILES_MAP.getContentType(file);
	}
}

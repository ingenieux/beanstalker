package br.com.ingenieux.mojo.cloudfront;

import static org.apache.commons.lang.StringUtils.stripStart;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import javax.activation.MimetypesFileTypeMap;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.s3.AmazonS3Client;

public abstract class AbstractCloudfrontMojo extends
		AbstractAWSMojo<AmazonCloudFrontClient> {

	@MojoParameter
	protected Distribution[] distributions;
	@MojoParameter(expression = "${project.build.directory}/${project.build.finalName}", required = true)
	protected File webappDirectory;
	protected AmazonS3Client s3Client;

	@Override
	protected void configure() {
		super.configure();
	
		try {
			this.s3Client = new AmazonS3Client(getAWSCredentials(),
					getClientConfiguration());
		} catch (MojoFailureException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected List<String> fetchLocalDistributionFiles(Distribution d) throws IOException {
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

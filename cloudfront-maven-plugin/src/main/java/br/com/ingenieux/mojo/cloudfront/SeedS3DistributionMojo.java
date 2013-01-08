package br.com.ingenieux.mojo.cloudfront;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.plugins.annotations.Mojo;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Mojo(name="seed-s3-distribution", requiresProject=true, requiresDirectInvocation=true)
public class SeedS3DistributionMojo extends AbstractCloudfrontMojo {
	protected Map<String, PutObjectRequest> results;

	public SeedS3DistributionMojo() {
		results = new TreeMap<String, PutObjectRequest>();
	}

	@Override
	protected Object executeInternal() throws Exception {
		for (Distribution d : distributions) {
			executeDistribution(d);
		}

		return results.size();
	}

	protected void executeDistribution(Distribution d) throws Exception {
		if (d.isS3Distribution()) {
			executeOnS3Distribution(d);
		} else {
			executeOnCustomDistribution(d);
		}

		fireDistributionExecuted(d);

		getLog().info("Total: " + results.size() + " objects");
	}

	protected void fireDistributionExecuted(Distribution d) {
	}

	protected void executeOnCustomDistribution(Distribution d)
			throws IOException {
		getLog().warn(
				"Distribution id "
						+ d.id
						+ " does not have an s3Bucket assigned (perhaps its a custom origin?). Skipping");
	}

	protected void executeOnS3Distribution(Distribution d) throws IOException {
		List<String> pathList = fetchLocalDistributionFiles(d);
		for (String path : pathList) {
			File file = new File(webappDirectory, path);

			String contentType = guessMimeType(file);

			Collection<PutObjectRequest> putObjectRequest = getPutObjectRequest(
					d, path, file, contentType);

			if (!putObjectRequest.isEmpty()) {
				for (PutObjectRequest req : putObjectRequest) {
					getLog().info(
							"Uploading into s3://" + d.s3Bucket + "/" + path);

					s3Client.putObject(req);

					results.put(req.getKey(), req);
				}
			} else {
				getLog().warn(
						"Skipping upload into s3://" + d.s3Bucket + "/" + path);
			}
		}
	}

	protected Collection<PutObjectRequest> getPutObjectRequest(Distribution d,
			String path, File file, String contentType) throws IOException {
		PutObjectRequest putObjectRequest = new PutObjectRequest(d.s3Bucket,
				path, file);

		putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

		ObjectMetadata objectMetadata = new ObjectMetadata();

		objectMetadata.setContentType(contentType);

		return Arrays.asList(putObjectRequest);
	}
}

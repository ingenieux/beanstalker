package br.com.ingenieux.mojo.cloudfront;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.strip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoRequiresProject;

import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.amazonaws.services.s3.model.PutObjectRequest;

@MojoGoal("update-distribution")
@MojoRequiresProject
public class UpdateDistributionMojo extends SeedS3DistributionMojo {
	private final HttpClient httpClient = new DefaultHttpClient();

	@Override
	protected void fireDistributionExecuted(Distribution d) {
		if (results.isEmpty())
			return;

		List<String> paths = new ArrayList<String>();

		InvalidationBatch batch = new InvalidationBatch("invalidate-" + d.id);
		CreateInvalidationRequest invalidationRequest = new CreateInvalidationRequest()
				.withDistributionId(d.id).withInvalidationBatch(batch);

		for (Map.Entry<String, PutObjectRequest> entry : results.entrySet()) {
			String key = entry.getKey();
			PutObjectRequest request = entry.getValue();

			if (null != request && request.getBucketName().equals(d.s3Bucket)) {
				paths.add("/" + request.getKey());
			} else {
				paths.add("/" + key);
			}
		}

		if (!paths.isEmpty()) {
			batch.setPaths(new Paths().withItems(paths));

			invalidationRequest.withInvalidationBatch(batch);

			getService().createInvalidation(invalidationRequest);
		} else {
			getLog().info("No need to invalidate. Skipping");
		}
	}

	@Override
	protected Collection<PutObjectRequest> getPutObjectRequest(Distribution d,
			String path, File file, String contentType) throws IOException {
		String md5sum = DigestUtils.md5Hex(new FileInputStream(file));

		String cacheETag = getETag(d, path);

		if (md5sum.equals(cacheETag)) {
			getLog().info("Ignoring resource " + path + " due to same eTag");
			return Collections.emptyList();
		}

		return super.getPutObjectRequest(d, path, file, contentType);
	}

	protected String getETag(Distribution d, String path) throws IOException,
			ClientProtocolException {
		if (!isBlank(d.s3Bucket))
			return s3Client.getObjectMetadata(d.s3Bucket, path).getETag();

		String assetUrl = String.format("http://%s/%s", d.domainName, path);

		getLog().info("Checking for eTag on " + assetUrl);

		HttpResponse result = httpClient.execute(new HttpHead(assetUrl));

		getLog().info("Status Code:" + result.getStatusLine().getStatusCode());

		if (result.containsHeader("ETag")) {
			String eTag = strip(result.getFirstHeader("ETag").getValue(), "\"");
			getLog().info("ETag is " + eTag);

			return eTag;
		}

		return "";
	}

	@Override
	protected void executeOnCustomDistribution(Distribution d)
			throws IOException {
		getLog().warn(
				"Distribution id "
						+ d.id
						+ " does not have an s3Bucket assigned (perhaps its a custom origin?). Scanning webappDirectory for matches");
		List<String> pathList = fetchLocalDistributionFiles(d);

		for (String path : pathList) {
			File file = new File(webappDirectory, path);

			String md5Sum = DigestUtils.md5Hex(new FileInputStream(file));
			String eTag = getETag(d, path);

			if (isBlank(eTag)) {
				getLog().info(
						"Resource "
								+ path
								+ " does not exist. No need to invalidate on a Custom Origin");
			} else if (!md5Sum.equals(eTag)) {
				getLog().info(
						"Resource "
								+ path
								+ " needs to be invalidated. Adding to Invalidation Request Queue");

				results.put(path, null);
			} else {
				getLog().info(
						"Resource "
								+ path
								+ " matches existing eTag. Not adding to invalidation queue");
			}
		}

	}
}

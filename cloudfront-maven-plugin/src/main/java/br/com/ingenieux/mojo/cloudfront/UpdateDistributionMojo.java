package br.com.ingenieux.mojo.cloudfront;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import com.amazonaws.services.s3.model.PutObjectRequest;

@MojoGoal("update-distribution")
@MojoRequiresProject
public class UpdateDistributionMojo extends SeedS3DistributionMojo {
	private final HttpClient httpClient = new DefaultHttpClient();

	// TODO: Handle Compressed Resources

	@Override
	protected void fireDistributionExecuted(Distribution d) {
		if (results.isEmpty())
			return;

		List<String> paths = new ArrayList<String>();

		InvalidationBatch batch = new InvalidationBatch("invalidate-" + d.id);
		CreateInvalidationRequest invalidationRequest = new CreateInvalidationRequest()
				.withDistributionId(d.id).withInvalidationBatch(batch);

		for (PutObjectRequest request : results.values())
			if (request.getBucketName().equals(d.s3Bucket))
				paths.add("/" + request.getKey());

		if (!paths.isEmpty()) {

			batch.setPaths(paths);

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

		HttpResponse result = httpClient.execute(new HttpHead(assetUrl));

		return result.getFirstHeader("ETag").getValue();
	}
}

package br.com.ingenieux.mojo.aws.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.internal.ProgressListenerChain;
import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils;
import com.amazonaws.services.s3.transfer.model.UploadResult;

public class BeanstalkerS3Client extends AmazonS3Client {
	private final class XProgressListener implements ProgressListener {
		private long contentLen;
		private Upload upload;

		private XProgressListener() {
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}

		public void setUpload(Upload upload) {
			this.upload = upload;
		}
		
		public Upload getUpload() {
			return upload;
		}

		@Override
		public void progressChanged(ProgressEvent e) {
			if (null == upload)
				return;

			TransferProgress xProgress = upload.getProgress();

			System.out.print("\r  "
					+ String.format("%.2f", xProgress.getPercentTransfered())
							+ "% " + asNumber(xProgress.getBytesTransfered())
							+ "/" + asNumber(contentLen) + BLANK_LINE);

			switch (e.getEventCode()) {
			case ProgressEvent.COMPLETED_EVENT_CODE: {
				System.out.println("Done");
				break;
			}
			case ProgressEvent.FAILED_EVENT_CODE: {
				try {
					AmazonClientException exc = upload.waitForException();

					System.err.println("Unable to upload file: "
							+ exc.getMessage());
				} catch (InterruptedException ignored) {
				}
				break;
			}
			}

		}
	}

	private static final String BLANK_LINE = StringUtils.repeat(" ", 24);

	public BeanstalkerS3Client() {
		super();
	}

	public String asNumber(long bytesTransfered) {
		// Extra Pedantry: I love *-ibytes
		return FileUtils.byteCountToDisplaySize(bytesTransfered).replaceAll(
				"B$", "iB");
	}

	public BeanstalkerS3Client(AWSCredentials awsCredentials,
			ClientConfiguration clientConfiguration) {
		super(awsCredentials, clientConfiguration);
	}

	public BeanstalkerS3Client(AWSCredentials awsCredentials) {
		super(awsCredentials);
	}

	public BeanstalkerS3Client(AWSCredentialsProvider credentialsProvider,
			ClientConfiguration clientConfiguration) {
		super(credentialsProvider, clientConfiguration);
	}

	public BeanstalkerS3Client(AWSCredentialsProvider credentialsProvider) {
		super(credentialsProvider);
	}

	@Override
	public PutObjectResult putObject(PutObjectRequest req)
			throws AmazonClientException, AmazonServiceException {
		TransferManager xManager = new TransferManager(this);
		TransferManagerConfiguration configuration = new TransferManagerConfiguration();
		configuration.setMultipartUploadThreshold(100 * Constants.KB);
		final long contentLen = TransferManagerUtils.getContentLength(req);

		XProgressListener progressListener = new XProgressListener();
		
		req.setProgressListener(new ProgressListenerChain(progressListener));

		xManager.setConfiguration(configuration);

		progressListener.setContentLen(contentLen);
		progressListener.setUpload(xManager.upload(req));

		UploadResult uploadResult = null;

		try {
			uploadResult = progressListener.getUpload().waitForUploadResult();
		} catch (InterruptedException e) {
			throw new AmazonClientException(e.getMessage(), e);
		}

		PutObjectResult result = new PutObjectResult();

		result.setETag(uploadResult.getETag());
		result.setVersionId(uploadResult.getVersionId());

		return result;
	}
}

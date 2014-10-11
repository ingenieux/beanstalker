package br.com.ingenieux.mojo.aws.util;

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
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.event.ProgressListenerChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Constants;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.TransferProgress;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

public class BeanstalkerS3Client extends AmazonS3Client {

  private static final String BLANK_LINE = StringUtils.repeat(" ", 24);
  private boolean multipartUpload = true;
  private boolean silentUpload = false;
  private TransferManager transferManager;

  public BeanstalkerS3Client() {
    super();
    init();
  }

  public BeanstalkerS3Client(AWSCredentials awsCredentials,
                             ClientConfiguration clientConfiguration) {
    super(awsCredentials, clientConfiguration);
    init();
  }

  public BeanstalkerS3Client(AWSCredentials awsCredentials) {
    super(awsCredentials);
    init();
  }

  public BeanstalkerS3Client(AWSCredentialsProvider credentialsProvider,
                             ClientConfiguration clientConfiguration) {
    super(credentialsProvider, clientConfiguration);
    init();
  }

  public BeanstalkerS3Client(AWSCredentialsProvider credentialsProvider) {
    super(credentialsProvider);
    init();
  }

  public boolean isMultipartUpload() {
    return multipartUpload;
  }

  public void setMultipartUpload(boolean multipartUploadP) {
    this.multipartUpload = multipartUploadP;
  }

  public boolean isSilentUpload() {
    return silentUpload;
  }

  public void setSilentUpload(boolean silentUpload) {
    this.silentUpload = silentUpload;
  }

  protected void init() {
    transferManager = new TransferManager(this);
    TransferManagerConfiguration configuration = new TransferManagerConfiguration();
    configuration.setMultipartUploadThreshold(100 * Constants.KB);
    transferManager.setConfiguration(configuration);
  }

  public TransferManager getTransferManager() {
    return transferManager;
  }

  public String asNumber(long bytesTransfered) {
    // Extra Pedantry: I love *-ibytes
    return FileUtils.byteCountToDisplaySize(bytesTransfered).replaceAll(
        "B$", "iB");
  }

  @Override
  public PutObjectResult putObject(PutObjectRequest req)
      throws AmazonClientException, AmazonServiceException {
    if (!multipartUpload) {
      return super.putObject(req);
    }

    final long contentLen = TransferManagerUtils.getContentLength(req);

    String tempFilename = req.getKey() + ".tmp";
    String origFilename = req.getKey();

    req.setKey(tempFilename);

    XProgressListener progressListener = new XProgressListener();

    req.setGeneralProgressListener(new ProgressListenerChain(progressListener));

    progressListener.setContentLen(contentLen);
    progressListener.setUpload(transferManager.upload(req));
    progressListener.setSilentUpload(silentUpload);

    try {
      progressListener.getUpload().waitForCompletion();
    } catch (InterruptedException e) {
      throw new AmazonClientException(e.getMessage(), e);
    }

    CopyObjectRequest copyReq = new CopyObjectRequest(req.getBucketName(), tempFilename,
                                                      req.getBucketName(), origFilename);

    copyObject(copyReq);

    deleteObject(new DeleteObjectRequest(req.getBucketName(), tempFilename));

    return null;
  }

  public void deleteMultiparts(String bucketName, Date since) {
    transferManager.abortMultipartUploads(bucketName, since);
  }

  private final class XProgressListener implements ProgressListener {

    private long contentLen;
    private Upload upload;
    private boolean silentUpload;

    public void setContentLen(long contentLen) {
      this.contentLen = contentLen;
    }

    public void setSilentUpload(boolean silentUpload) {
      this.silentUpload = silentUpload;
    }

    public Upload getUpload() {
      return upload;
    }

    public void setUpload(Upload upload) {
      this.upload = upload;
    }

    @Override
    public void progressChanged(ProgressEvent e) {
      if (null == upload) {
        return;
      }

      TransferProgress xProgress = upload.getProgress();

      if (!silentUpload) {
        System.out.print("\r  "
                         + String.format("%.2f", xProgress.getPercentTransferred())
                         + "% " + asNumber(xProgress.getBytesTransferred()) + "/"
                         + asNumber(contentLen) + BLANK_LINE);
      }

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
}

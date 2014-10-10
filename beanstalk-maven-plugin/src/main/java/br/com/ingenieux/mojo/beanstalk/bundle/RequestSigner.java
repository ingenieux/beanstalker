package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class RequestSigner {

  public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
      "yyyyMMdd'T'HHmmss");
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
      "yyyyMMdd");
  private static final String TERMINATOR = "aws4_request";
  private static final String SCHEME = "AWS4";

  private static final String AWS_ALGORITHM = "HMAC-SHA256";

  static {
    SimpleTimeZone timezone = new SimpleTimeZone(0, "UTC");

    DATE_TIME_FORMAT.setTimeZone(timezone);
    DATE_FORMAT.setTimeZone(timezone);
  }

  AWSCredentials awsCredentials;

  String applicationId;

  String region;

  String commitId;

  String environmentName;

  Date date;

  String service = "devtools";

  private String strDate;

  private String strDateTime;

  public RequestSigner(AWSCredentialsProvider awsCredentials, String applicationId,
                       String region, String commitId, String environmentName, Date date) {
    super();
    this.awsCredentials = awsCredentials.getCredentials();
    this.applicationId = applicationId;
    this.region = region;
    this.commitId = commitId;
    this.environmentName = environmentName;
    this.date = date;
  }

  public String getPushUrl() {
    strDate = DATE_FORMAT.format(date);

    strDateTime = DATE_TIME_FORMAT.format(date);

    String user = awsCredentials.getAWSAccessKeyId();

    String host = String.format("git.elasticbeanstalk.%s.amazonaws.com",
                                region);

    String path = String.format("/v1/repos/%s/commitid/%s",
                                hexEncode(applicationId), hexEncode(commitId));

    if (isNotBlank(environmentName)) {
      path += String.format("/environment/%s", hexEncode(environmentName));
    }

    String scope = String.format("%s/%s/%s/%s", strDate,
                                 region, service, TERMINATOR);

    StringBuilder stringToSign = new StringBuilder();

    stringToSign.append(String.format("%s-%s\n%s\n%s\n", SCHEME,
                                      AWS_ALGORITHM, strDateTime, scope));

    stringToSign.append(DigestUtils.sha256Hex(String.format(
        "GIT\n%s\n\nhost:%s\n\nhost\n", path, host).getBytes()));

    byte[] key = deriveKey();

    byte[] digest = hash(key, stringToSign.toString());

    String signature = Hex.encodeHexString(digest);

    String password = strDateTime.concat("Z").concat(signature);

    String returnUrl = String.format("https://%s:%s@%s%s", user, password,
                                     host, path);

    return returnUrl;
  }

  private byte[] deriveKey() {
    String secret = SCHEME.concat(awsCredentials.getAWSSecretKey());
    byte[] kSecret = secret.getBytes();
    byte[] kDate = hash(kSecret, strDate);
    byte[] kRegion = hash(kDate, region);
    byte[] kService = hash(kRegion, service);
    byte[] key = hash(kService, TERMINATOR);
    return key;
  }

  private byte[] hash(byte[] kSecret, String obj) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(kSecret, "HmacSHA256");

      Mac mac = Mac.getInstance("HmacSHA256");

      mac.init(keySpec);

      return mac.doFinal(obj.getBytes("UTF-8"));
    } catch (Exception exc) {
      throw new RuntimeException(exc);
    }
  }

  private String hexEncode(String obj) {
    return Hex.encodeHexString(obj.getBytes());
  }
}

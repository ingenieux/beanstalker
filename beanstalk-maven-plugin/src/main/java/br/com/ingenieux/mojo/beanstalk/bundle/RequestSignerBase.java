package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.auth.AWSCredentials;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * Created by aldrin on 04/01/16.
 */
public class RequestSignerBase {
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
        "yyyyMMdd'T'HHmmss");

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
        "yyyyMMdd");

    protected static final String AWS_ALGORITHM = "HMAC-SHA256";

    protected static final String TERMINATOR = "aws4_request";

    protected static final String SCHEME = "AWS4";

    static {
        SimpleTimeZone timezone = new SimpleTimeZone(0, "UTC");

        DATE_TIME_FORMAT.setTimeZone(timezone);
        DATE_FORMAT.setTimeZone(timezone);
    }

    final AWSCredentials awsCredentials;

    final String region;

    final String service;

    final Date date;

    final String strDate;

    final String strDateTime;

    protected RequestSignerBase(AWSCredentials awsCredentials, String region, String service, Date date) {
        this.awsCredentials = awsCredentials;
        this.region = region;
        this.service = service;
        this.date = date;
        this.strDate = DATE_FORMAT.format(date);
        this.strDateTime = DATE_TIME_FORMAT.format(date);
    }

    protected byte[] deriveKey() {
      String secret = RequestSigner.SCHEME.concat(awsCredentials.getAWSSecretKey());
      byte[] kSecret = secret.getBytes();
      byte[] kDate = hash(kSecret, strDate);
      byte[] kRegion = hash(kDate, region);
      byte[] kService = hash(kRegion, service);
      byte[] key = hash(kService, RequestSigner.TERMINATOR);
      return key;
    }

    protected byte[] hash(byte[] kSecret, String obj) {
      try {
        SecretKeySpec keySpec = new SecretKeySpec(kSecret, "HmacSHA256");

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(keySpec);

        return mac.doFinal(obj.getBytes("UTF-8"));
      } catch (Exception exc) {
        throw new RuntimeException(exc);
      }
    }

    protected String hexEncode(String obj) {
      return Hex.encodeHexString(obj.getBytes());
    }
}

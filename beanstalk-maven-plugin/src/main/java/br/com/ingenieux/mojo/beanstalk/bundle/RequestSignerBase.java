/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.auth.AWSCredentials;

import org.apache.commons.codec.binary.Hex;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by aldrin on 04/01/16.
 */
public class RequestSignerBase {
  public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

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

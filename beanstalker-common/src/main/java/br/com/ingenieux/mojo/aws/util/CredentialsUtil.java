package br.com.ingenieux.mojo.aws.util;

import static org.apache.commons.lang.StringUtils.defaultString;

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
 * Utilities for AWS Credentials
 *
 * @author aldrin
 */
public class CredentialsUtil {

  /**
   * <p> Huge thanks to Eric Hammond from Alestic on this one (source: <a
   * href="http://alestic.com/2009/11/ec2-credentials">Understanding Access Credentials for
   * AWS/EC2</a>: </p>
   *
   * <p> (6) AWS Access Key ID and (7) Secret Access Key. This is the first of two pairs of
   * credentials which can be used to access and control basic AWS services through the API
   * including EC2, S3, SimpleDB, CloudFront, SQS, EMR, RDS, etc. Some interfaces use this pair, and
   * some use the next pair below. Pay close attention to the names requested. The Access Key ID is
   * 20 alpha-numeric characters like 022QF06E7MXBSH9DHM02 and is not secret; it is available to
   * others in some situations. <b>The Secret Access Key is 40 alpha-numeric-slash-plus characters
   * like <code>kWcrlUX5JEDGM/LtmEENI/aVmYvHNif5zB+d9+ct</code> and must be kept very secret</b>.
   * </p>
   *
   * @param s string to replace
   * @return redacted string
   */
  public static String redact(String s) {
    s = defaultString(s);

    if (-1 != s.indexOf("git-")) {
      return s;
    }

    return s.replaceAll("[\\p{Alnum}\\/\\+]{40}", "/***REDACTED POSSIBLE AWS CREDENTIAL***/");
  }
}

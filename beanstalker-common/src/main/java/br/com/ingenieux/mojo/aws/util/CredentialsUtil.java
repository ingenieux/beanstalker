package br.com.ingenieux.mojo.aws.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public static final Pattern PATTERN_ALNUM_40 = Pattern.compile("[\\p{Alnum}\\/\\+]{40}");

  public static final Pattern PATTERN_HEX_40 = Pattern.compile("[\\p{XDigit}\\/\\+]{40}", Pattern.CASE_INSENSITIVE);
  public static final String MESSAGE = "/** REDACTED POSSIBLE AWS CREDENTIAL **/";

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

    StringBuilder stringBuilder = new StringBuilder(s);

    boolean found;

    int lastPos = 0;

    do {
      final Matcher matcher = PATTERN_ALNUM_40.matcher(stringBuilder);

      found = matcher.find(lastPos);

      if (found) {
        CharSequence segment = stringBuilder.subSequence(matcher.start(), matcher.end());

        if (! PATTERN_HEX_40.matcher(segment).matches()) {
          stringBuilder.replace(matcher.start(), matcher.end(), MESSAGE);

          lastPos = matcher.start() + MESSAGE.length();
        } else {
          lastPos = matcher.end();
        }
      }
    } while (found);

    return stringBuilder.toString();
  }

  public static void main(String[] args) throws Exception {
    System.out.println(redact("{\n" +
            "  \"accessKey\": \"0THISISANACCESSKEYh3\",\n" +
            "  \"secretKey\": \"abc123abdefasad32ldasdlj323lkjaR+secretk\",\n" +
            "  \"applicationName\": \"multipackage-example\",\n" +
            "  \"commitId\": \"73031a04846d8adaee6fc1eb1b4bb98af9878c3b\",\n" +
            "  \"repoName\": \"ingenieux-image-blobs\",\n" +
            "  \"targetPath\": \"s3://elasticbeanstalk-us-east-1-235368163414/apps/multipackage-example/versions/git-73031a04846d8adaee6fc1eb1b4bb98af9878c3b.zip\"\n" +
            "}"));
  }
}

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
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import static org.junit.Assert.assertEquals;

@Ignore
public class RequestBuilderTest {

  private RequestSigner requestSigner;

  @Before
  public void setUp() throws Exception {
    //this.requestSigner = new RequestSigner(creds, );

    /*
    requestSigner.applicationId = "readability-metrics-aws";

    //requestSigner.awsCredentials = creds;

    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

    requestSigner.date = dateTimeFormat.parse("20121128T104956Z");

    //requestSigner.region = "us-east-1";
    requestSigner.commitId = "cf9b20486b9b3bedc32276d8cee21e57db1987e2";
    requestSigner.environmentName = "rm-aws";
    */
  }

  @Test
  public void testEquality() throws Exception {
/*
    assertEquals(
        requestSigner.getPushUrl(),
        "https://heygetarealkey:20121128T104956Z2d9d5c5609b3e5221759fc17ed487d4cf833a23a59d26747441fe9bbb056d488@git.elasticbeanstalk.us-east-1.amazonaws.com/v1/repos/726561646162696c6974792d6d6574726963732d617773/commitid/63663962323034383662396233626564633332323736643863656532316535376462313938376532/environment/726d2d617773");
*/
  }

  @Test
  public void testCodeCommit() throws Exception {
    CodeCommitRequestSigner signer = new CodeCommitRequestSigner(new DefaultAWSCredentialsProviderChain(), "ingenieux-image-blobs", RequestSignerBase.DATE_TIME_FORMAT.parse("20160105T031736Z"));

    String pushUrl = signer.getPushUrl();

    assertEquals(pushUrl, "https://0SB93DDYBE63367703R2:20160105T031736Z93cb4b090d8642fbe6772a5d1f8320bed5ec892195cafa6ce57d290d83b79b2a@git-codecommit.us-east-1.amazonaws.com/v1/repos/ingenieux-image-blobs");
  }

}

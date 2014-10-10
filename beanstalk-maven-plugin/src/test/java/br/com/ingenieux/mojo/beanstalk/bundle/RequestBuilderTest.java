package br.com.ingenieux.mojo.beanstalk.bundle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

import static org.junit.Assert.assertEquals;

@Ignore
public class RequestBuilderTest {

  private RequestSigner requestSigner;

  @Before
  public void setUp() throws Exception {
    //AWSCredentials creds = new BasicAWSCredentials(...);

    //this.requestSigner = new RequestSigner(creds, );

    requestSigner.applicationId = "readability-metrics-aws";
    //requestSigner.awsCredentials = creds;

    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

    requestSigner.date = dateTimeFormat.parse("20121128T104956Z");

    requestSigner.region = "us-east-1";
    requestSigner.commitId = "cf9b20486b9b3bedc32276d8cee21e57db1987e2";
    requestSigner.environmentName = "rm-aws";
  }

  @Test
  public void testEquality() throws Exception {
    assertEquals(
        requestSigner.getPushUrl(),
        "https://heygetarealkey:20121128T104956Z2d9d5c5609b3e5221759fc17ed487d4cf833a23a59d26747441fe9bbb056d488@git.elasticbeanstalk.us-east-1.amazonaws.com/v1/repos/726561646162696c6974792d6d6574726963732d617773/commitid/63663962323034383662396233626564633332323736643863656532316535376462313938376532/environment/726d2d617773");
  }

}

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.healthcheck;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;

import javax.inject.Inject;

import br.com.ingenieux.dropwizard.guice.InjectableHealthCheck;
import ${package}.ServiceConfiguration;

public class AWSHealthCheck extends InjectableHealthCheck {

  @Inject
  ServiceConfiguration cfg;

  @Inject
  AmazonS3 s3;

  @Inject
  AmazonDynamoDB dynamoDB;

  @Override
  protected Result check() throws Exception {
    if (SystemUtils.IS_OS_UNIX) {
      if (new File("/tmp").getFreeSpace() < FileUtils.ONE_GB) {
        return Result.unhealthy("Not enough free space");
      }
    }

    return Result.healthy();
  }

  @Override
  public String getName() {
    return "aws";
  }
}

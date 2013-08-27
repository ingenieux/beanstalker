#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.time.StopWatch;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Inject;

public class HealthResource extends BaseResource {
	@Inject
	AmazonEC2 ec2;

	@Inject
	AmazonS3 s3;

	@Inject
	AmazonDynamoDB dynamoDb;
	
	@HEAD
	@Produces("text/plain")
	@Path("/check")
	public String doHeadOnHealthCheck() {
		return doHealthCheck();
	}
	

	@GET
	@Produces("text/plain")
	@Path("/check")
	public String doHealthCheck() {
		StopWatch stopWatch = new StopWatch();
		
		stopWatch.start();
		
		try {
			ec2.describeInstances();

			s3.listBuckets();

			dynamoDb.listTables();
		} catch (Exception exc) {
			if (logger.isWarnEnabled())
				logger.warn("doHealthCheck() failed", exc);
			
			throw new WebApplicationException(exc, 500);
		}
		
		stopWatch.stop();
		
		long len = stopWatch.getTime() / 1000;
		
		if (logger.isInfoEnabled())
			logger.info("doHealthCheck(): Took {}s", new Object[] { len } );

		return "OK: " + len;
	}

}

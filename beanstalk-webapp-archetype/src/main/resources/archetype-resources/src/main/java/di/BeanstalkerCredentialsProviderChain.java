#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.di;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;

/**
 * <p>
 * Elastic Beanstalk Propagates the AWS Credentials in a Way that is not
 * supported by the AWS SDK
 * </p>
 * 
 * <p>
 * We also love having aws.properties for local usage.
 * </p>
 * 
 * <p>
 * So here's a midterm: Combining AWS Credentials Provider Chain with whatever
 * elastic beanstalk needs
 * </p>
 * 
 */
public class BeanstalkerCredentialsProviderChain extends
		AWSCredentialsProviderChain {
	static class ElasticBeanstalkCredentialsProvider implements
			AWSCredentialsProvider {
		@Override
		public AWSCredentials getCredentials() {
			String awsAccessKeyId = System.getProperty("AWS_ACCESS_KEY_ID");
			String awsSecretKey = System.getProperty("AWS_SECRET_KEY");

			if (isNotBlank(awsAccessKeyId) && isNotBlank(awsSecretKey))
				return new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);

			throw new AmazonClientException(
					"Unable to load AWS credentials from Java system properties (AWS_ACCESS_KEY_ID and AWS_SECRET_KEY)");
		}

		@Override
		public void refresh() {
		}
	}

	public BeanstalkerCredentialsProviderChain() {
		super(new ElasticBeanstalkCredentialsProvider(),
				new EnvironmentVariableCredentialsProvider(),
				new SystemPropertiesCredentialsProvider(),
				new ClasspathPropertiesFileCredentialsProvider(
						"/aws.properties"),
				new InstanceProfileCredentialsProvider());
	}
}

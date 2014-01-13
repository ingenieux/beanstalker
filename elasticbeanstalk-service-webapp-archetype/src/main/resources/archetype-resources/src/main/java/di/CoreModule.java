#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.di;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ingenieux.cloudy.awseb.di.BaseAWSModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CoreModule extends AbstractModule {
	@Provides
	public ObjectMapper getObjectMapper() {
		return new ObjectMapper();
	}
	
    @Override
    protected void configure() {
    	install(new BaseAWSModule().withDynamicRegion());
        // Uncomment to create beanstalk awareness
        // install(new EC2Module());
        // install(new BeanstalkEnvironmentModule());
    }
}

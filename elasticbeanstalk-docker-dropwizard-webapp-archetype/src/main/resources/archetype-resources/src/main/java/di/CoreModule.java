#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import com.netflix.governator.guice.lazy.LazySingleton;

import org.apache.http.client.HttpClient;

import javax.inject.Inject;

import br.com.ingenieux.cloudy.awseb.di.BaseAWSModule;
import ${package}.ServiceConfiguration;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;


public class CoreModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new BaseAWSModule().withDynamicRegion());
  }

  @Provides
  @LazySingleton
  @Inject
  public HttpClient getHttpClient(Environment env, ServiceConfiguration cfg) {
    return new HttpClientBuilder(env).using(cfg.getHttpClientConfiguration())
        .build("http-client");
  }
}

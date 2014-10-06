#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;

public class ServiceConfiguration extends Configuration {

  @JsonProperty
  protected String graphiteHost = null;
  @Valid
  @NotNull
  @JsonProperty
  private HttpClientConfiguration httpClient = new HttpClientConfiguration();

  public String getGraphiteHost() {
    return graphiteHost;
  }

  public HttpClientConfiguration getHttpClientConfiguration() {
    return httpClient;
  }
}

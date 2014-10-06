#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resources;

import com.codahale.metrics.health.HealthCheck;

import org.apache.commons.lang.SystemUtils;

import java.util.Map;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.dropwizard.setup.Environment;

@Path("/health")
@Produces("application/json; charset=utf-8")
public class HealthCheckResource extends BaseResource {

  @Inject
  Environment environment;


  /*
  @GET
  @Path("/env")
  public Map<String, String> getEnv() {
    return System.getenv();
  }
  @Inject
  AssetServiceConfiguration cfg;

  @GET
  @Path("/config")
  public AssetServiceConfiguration getConfig() {
    return cfg;

  }
  */

  @GET
  @Path("/check")
  public Response doCheck() throws Exception {
    SortedMap<String, HealthCheck.Result>
        healthChecks =
        environment.healthChecks().runHealthChecks();

    boolean bValid = true;

    for (Map.Entry<String, HealthCheck.Result> aHealthCheck : healthChecks.entrySet()) {
      bValid &= aHealthCheck.getValue().isHealthy();

      if (!bValid) {
        if (SystemUtils.IS_OS_UNIX) {
          logger.info("Shutting down due to lack of health");

          try {
            environment.getApplicationContext().getServer().stop();
          } catch (Exception exc) {

          }

          System.exit(0);
        }
        break;
      }
    }

    return Response.ok().build();
  }
}

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.codahale.metrics.JmxReporter;

import br.com.ingenieux.dropwizard.guice.GuiceBundle;
import br.com.ingenieux.dropwizard.interpolation.EnvironmentVariableInterpolationBundle;
import ${package}.di.CoreModule;
import ${package}.di.WebModule;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Main extends Application<ServiceConfiguration> {

  public static void main(String[] args) throws Exception {
    new Main().run(args);
  }

  @Override
  public String getName() {
    return "${artifactId}";
  }

  @Override
  public void initialize(Bootstrap<ServiceConfiguration> bootstrap) {
    bootstrap.addBundle(new EnvironmentVariableInterpolationBundle());

    final GuiceBundle.Builder<ServiceConfiguration>
        configBuilder =
        GuiceBundle.<ServiceConfiguration>newBuilder()
            .addModule(new CoreModule())
            .addModule(new WebModule())
            .enableAutoConfig("${package}")
            .setConfigClass(ServiceConfiguration.class);

    GuiceBundle<ServiceConfiguration> guiceBundle = configBuilder
        .build();

    bootstrap.addBundle(guiceBundle);
  }

  @Override
  public void run(ServiceConfiguration cfg,
                  Environment env) throws Exception {
    JmxReporter.forRegistry(env.metrics()).build().start();

    /**
     if (isNotBlank(System.getenv("GRAPHITE_HOST"))) {
     String[] elts = System.getenv("GRAPHITE_HOST").split("${symbol_escape}${symbol_escape}Q:${symbol_escape}${symbol_escape}E");

     Graphite graphite = new Graphite(new InetSocketAddress(elts[0], Integer.valueOf(elts[1])));

     logger.info("Enabling graphite logging to {}:{} (key: {})", elts[0], elts[1], elts[2]);

     GraphiteReporter.forRegistry(env.metrics()).prefixedWith(elts[2]).build(graphite).start(30, TimeUnit.SECONDS);
     }

    ServletRegistration.Dynamic jolokia = env.servlets().addServlet("jolokia", AgentServlet.class);

    jolokia.addMapping("/jolokia/*");
     */
  }
}

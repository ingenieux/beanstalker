#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.di;

import com.google.inject.servlet.ServletModule;

public class WebModule extends ServletModule {

  @Override
  protected void configureServlets() {
    /*
    serve("/public/*").with(AssetServlet.class);
    serve("/thumbs/*").with(ThumborServlet.class);
    serve("/tasks/*").with(TaskForwardingServlet.class);
    */
  }
}

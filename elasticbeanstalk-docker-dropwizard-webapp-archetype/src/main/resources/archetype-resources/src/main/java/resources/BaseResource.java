#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resources;

import com.google.inject.Injector;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class BaseResource {

  public static final String ID_MASK = "{id}";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  protected ObjectMapper objectMapper;

  @Inject
  protected Injector injector;

  protected <K extends BaseResource> K createResource(Class<K> clazz, Object... args)
      throws Exception {
    if (null != injector.getBinding(clazz)) {
      return injector.getInstance(clazz);
    }

    @SuppressWarnings("unchecked")
    K result = (K) ConstructorUtils.invokeConstructor(clazz, args);

    injector.injectMembers(result);

    return result;
  }
}

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * Base Class of All JAX-RS Resources
 * 
 * Use this as a suitable Extension Point :)
 */
public class BaseResource {
	public static final String ID_MASK = "{ id: ${symbol_escape}${symbol_escape}w+ }";
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected Injector injector;
	
	protected <K extends BaseResource> K createResource(Class<K> clazz, Object... args) throws Exception {
		if (null != injector.getBinding(clazz))
			return injector.getInstance(clazz);
		
		@SuppressWarnings("unchecked")
		K result = (K) ConstructorUtils.invokeConstructor(clazz, args);
		
		injector.injectMembers(result);
		
		return result;
	}
	
	@GET
	@Produces("text/plain")
	@Path("/info")
	public String getResourceClass() {
		return getClass().getName();
	}
}

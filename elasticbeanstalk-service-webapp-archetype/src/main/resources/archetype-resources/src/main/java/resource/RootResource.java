#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resource;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Root Resource Class. Represents a single entry-point for the whole REST
 * Application, in order to ease on maintenance
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootResource extends BaseResource {
	@Path("/debug")
	@GET
	public ObjectNode getAllHeaders(@Context HttpHeaders httpHeaders) {
		ObjectNode result = objectMapper.createObjectNode();

		Set<Entry<String, List<String>>> entrySet = httpHeaders
				.getRequestHeaders().entrySet();

		for (Entry<String, List<String>> entry : entrySet) {
			String key = entry.getKey();
			JsonNode value = null;

			if (1 == entry.getValue().size()) {
				value = new TextNode(entry.getValue().get(0));
			} else {
				ArrayNode arrayNode = objectMapper.createArrayNode();

				for (String v : entry.getValue())
					arrayNode.add(v);

				value = arrayNode;
			}

			result.put(key, value);
		}

		return result;
	}
	
	@Path("/remote")
	@GET
	public String getRemoteAddress(@Context HttpServletRequest request) {
		return request.getRemoteHost();
	}
	
	@Path("/health")
	public HealthResource getHealthResource() throws Exception {
		return super.createResource(HealthResource.class);
	}

}

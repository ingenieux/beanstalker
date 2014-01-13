#set( $symbol_pound = '#' )
        #set( $symbol_dollar = '$' )
        #set( $symbol_escape = '\' )
package ${package}.resource;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Path("/sqsd")
public class SampleSQSDResource extends AbstractSQSDResource {
    private final Logger logger = LoggerFactory.getLogger(SampleSQSDResource.class);

    @Override
    protected Response executeInternal(HttpHeaders headers, ObjectNode bodyNode) {
        logger.info("headers: {}, body {}", headers, bodyNode);

        return Response.ok().build();
    }
}
#set( $symbol_pound = '#' )
        #set( $symbol_dollar = '$' )
        #set( $symbol_escape = '\' )
package ${package}.resource;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Date;

@Path("/sqsd")
public class SampleSQSDResource extends AbstractSQSDResource {
    private final Logger logger = LoggerFactory.getLogger(SampleSQSDResource.class);

    @Override
    protected Response executeInternal(String msgId, String queueName, Date receivedAt, Integer receiveCount, ObjectNode bodyNode) {
        logger.info("received message with msgId {}, queueName {}, at {}, receivedCount {}, body {}", msgId, queueName, receivedAt, receiveCount, bodyNode);

        return Response.ok().build();
    }
}

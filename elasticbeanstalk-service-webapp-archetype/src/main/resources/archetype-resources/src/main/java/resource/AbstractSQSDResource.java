#set( $symbol_pound = '#' )
        #set( $symbol_dollar = '$' )
        #set( $symbol_escape = '\' )
package ${package}.resource;

import javax.ws.rs.Path;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

/**
 * Represents a Base SQSD Resource
 */
public abstract class AbstractSQSDResource extends BaseResource {
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Response execute(@HeaderParam("X-aws-sqsd-msgid") String msgId,
                            @HeaderParam("X-aws-sqsd-queue") String queueName,
                            @HeaderParam("X-aws-sqsd-first-received-at") Date receivedAt,
                            @HeaderParam("X-aws-sqsd-receive-count") Integer receiveCount,
                            ObjectNode bodyNode) throws Exception {
        return executeInternal(msgId, queueName, receivedAt, receiveCount, bodyNode);
    }

    protected abstract Response executeInternal(String msgId, String queueName, Date receivedAt, Integer receiveCount, ObjectNode bodyNode);
}

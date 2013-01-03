#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.codehaus.jackson.node.ObjectNode;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;

/**
 * Represents a Minimalistic SNS Resource Template
 * 
 * @author aldrin
 */
public class BaseSNSResource extends BaseResource {
	/**
	 * Message Type: Subscription Confirmation
	 */
	public static final String SUBSCRIPTION_CONFIRMATION_MESSAGE_TYPE = "SubscriptionConfirmation";

	/**
	 * Message Type: Notification
	 */
	public static final String NOTIFICATION_MESSAGE_TYPE = "Notification";
	
	/**
	 * Message Type: Unsubscribe Confirmation
	 */
	public static final String UNSUBSCRIBE_CONFIRMATION_MESSAGE_TYPE = "UnsubscribeConfirmation";
	
	/**
	 * Header Type: Message Type
	 */
	public static final String MESSAGE_TYPE_HEADER = "x-amz-sns-message-type";
	
	/**
	 * Header Type: Subscription ARN
	 */
	public static final String SUBSCRIPTION_ARN_HEADER = "x-amz-sns-subscription-arn";
	
	/**
	 * Header Type: Topic ARN
	 */
	public static final String TOPIC_ARN_HEADER = "X-Amz-Sns-Topic-Arn";
	
	/**
	 * SNS Client
	 */
	@Inject
	protected AmazonSNS snsClient;

	public BaseSNSResource() {
		super();
	}

	/**
	 * Main Handler
	 * 
	 * @param endpointId endpoint id
	 * @param messageType message type
	 * @param subscriptionArn subscription arn
	 * @param topicArn topic arn
	 * @param body message payload (json
	 * @throws Exception something has happened :)
	 */
	@POST
	@Consumes("text/plain")
	@Path("/" + ID_MASK)
	public void onSNSMessage(@PathParam("id") String endpointId, @HeaderParam(MESSAGE_TYPE_HEADER) String messageType, @HeaderParam(SUBSCRIPTION_ARN_HEADER) String subscriptionArn,
			@HeaderParam(TOPIC_ARN_HEADER) String topicArn, String body) throws Exception {
				ObjectNode bodyNode = (ObjectNode) objectMapper.readTree(body);
			
				if (SUBSCRIPTION_CONFIRMATION_MESSAGE_TYPE.equals(messageType)) {
					String token = bodyNode.get("Token").getTextValue();
			
					if (logger.isInfoEnabled())
						logger.info(
								"Received SNS Subscription Confirmation (topicArn: {}, endpointId: {}, token: {})",
								new Object[] { topicArn, token });
			
					ConfirmSubscriptionResult subscriptionResult = snsClient
							.confirmSubscription(new ConfirmSubscriptionRequest(
									topicArn, token));
			
					if (logger.isInfoEnabled())
						logger.info("Subscription Result: {}",
								new Object[] { subscriptionResult });
				} else if (NOTIFICATION_MESSAGE_TYPE.equals(messageType)) {
					handleNotification(endpointId, bodyNode);
				} else if (UNSUBSCRIBE_CONFIRMATION_MESSAGE_TYPE.equals(messageType)) {
					handleUnsubscribe(endpointId, bodyNode);
				}
			
			}

	/**
	 * Here you receive a message whenever you get unsubscribed to a topic
	 * 
	 * @param endpointId
	 *            endpoint id
	 * @param bodyNode
	 *            body
	 */
	public void handleUnsubscribe(String endpointId, ObjectNode bodyNode) {
		if (logger.isInfoEnabled()) {
			logger.info(
					"Handling notification unsubscribe (endpointId: {}, bodyNode: {})",
					new Object[] { endpointId, bodyNode });
		}
	}

	/**
	 * This is actually where you override and add behaviour
	 * 
	 * @param endpointId
	 *            endpoint id
	 * @param bodyNode
	 *            full message payload (as json)
	 */
	public void handleNotification(String endpointId, ObjectNode bodyNode) {
		String subjectStr = bodyNode.get("Subject").getTextValue();
		String messageStr = bodyNode.get("Message").getTextValue();
	
		if (logger.isInfoEnabled()) {
			logger.info(
					"Handling notification (endpointId: {}, subject: {}, message: {}, bodyNode: {})",
					new Object[] { endpointId, subjectStr, messageStr, bodyNode });
		}
	}
}
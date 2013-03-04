#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.resource;

import java.io.InputStream;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

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
import com.amazonaws.services.sns.util.SignatureChecker;

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
                
                validateSignature(body, bodyNode);
            
                if (SUBSCRIPTION_CONFIRMATION_MESSAGE_TYPE.equals(messageType)) {
                    handleSubscribe(endpointId, topicArn, bodyNode);
                } else if (NOTIFICATION_MESSAGE_TYPE.equals(messageType)) {
                    handleNotification(endpointId, bodyNode);
                } else if (UNSUBSCRIBE_CONFIRMATION_MESSAGE_TYPE.equals(messageType)) {
                    handleUnsubscribe(endpointId, bodyNode);
                }
            
            }
    
    private static final SignatureChecker SIGNATURE_CHECKER = new SignatureChecker();
    
    private static final Pattern PATTERN_SNS_KEY = Pattern
            .compile("^sns\\.[\\-\\w]+\\.amazonaws.com$");    

    private static final Map<String, X509Certificate> SNS_CERT_MAP = new TreeMap<String, X509Certificate>();

    protected void validateSignature(String body, ObjectNode bodyNode) throws Exception {
        String signingCert = bodyNode.get("SigningCertURL").getTextValue();
        
        X509Certificate cert = SNS_CERT_MAP.get(signingCert);
        boolean expired = false;
        
        if (null != cert) {
            /*
             * Test Certificate Expiry
             */
            final X509Certificate tmpCert = cert;
            final Date now = new Date();
            @SuppressWarnings("serial")
            Set<Date> dateSet = new TreeSet<Date>() {{
                add(tmpCert.getNotBefore());
                add(now);
                add(tmpCert.getNotAfter());
            }};
            List<Date> dateList = new ArrayList<Date>(dateSet);
        
            expired = (!dateList.get(1).equals(now));
        }
        
        if (null == cert || expired) {
            URL signingCertURL = new URL(signingCert);
            
            {
                // Validates Original SigningCert Host
                if (!"https".equals(signingCertURL.getProtocol()))
                    throw new IllegalStateException("Illegal Protocol for SigningCertURL");
                
                String signingCertHost = signingCertURL.getHost();
                
                if (!PATTERN_SNS_KEY.matcher(signingCertHost).matches())
                    throw new IllegalStateException("Illegal Host for SigningCertHost: " + signingCertHost);
            }
            
            InputStream inputStream = signingCertURL.openStream();
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(inputStream);
            
            inputStream.close();
            
            SNS_CERT_MAP.put(signingCert, cert);
        }
        
        String signature = bodyNode.get("Signature").getTextValue();
        
        PublicKey publicKey = cert.getPublicKey();

        SIGNATURE_CHECKER.verifySignature(body, signature, publicKey);
    }

    public void handleSubscribe(String endpointId, String topicArn, ObjectNode bodyNode) {
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
    public void handleNotification(String endpointId, ObjectNode bodyNode) throws Exception {
        String subjectStr = bodyNode.get("Subject").getTextValue();
        String messageStr = bodyNode.get("Message").getTextValue();
    
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Handling notification (endpointId: {}, subject: {}, message: {}, bodyNode: {})",
                    new Object[] { endpointId, subjectStr, messageStr, bodyNode });
        }
    }
}
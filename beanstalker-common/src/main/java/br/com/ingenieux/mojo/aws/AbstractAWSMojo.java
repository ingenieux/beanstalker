package br.com.ingenieux.mojo.aws;

import br.com.ingenieux.mojo.aws.util.AWSClientFactory;
import br.com.ingenieux.mojo.aws.util.TypeUtil;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Represents a Mojo which keeps AWS passwords
 * <p/>
 * TODO: Refactor into tiny, delegated classes. Currently its a huge bloat, but it works, right?
 * <p/>
 * <p> <b>NOTE:</b> Settings in this class use properties based in "beanstalker", which is the
 * project. The beanstalk module, though, prefixes then as "beanstalk" instead </p>
 * <p/>
 * Parts of this class come from <a href="http://code.google.com/p/maven-gae-plugin">maven-gae-plugin</a>'s
 * source code.
 */
public abstract class AbstractAWSMojo<S extends AmazonWebServiceClient> extends
        AbstractMojo
        implements Contextualizable {

    private static final String
            SECURITY_DISPATCHER_CLASS_NAME =
            "org.sonatype.plexus.components.sec.dispatcher.SecDispatcher";
    /**
     * Maven Settings Reference
     */
    @Parameter(property = "settings", required = true, readonly = true)
    protected Settings settings;
    /**
     * AWS Credentials
     */
    protected AWSCredentialsProvider awsCredentialsProvider;
    /**
     * The server id in maven settings.xml to use for AWS Services Credentials (accessKey /
     * secretKey)
     */
    @Parameter(property = "beanstalker.serverId", defaultValue = "aws.amazon.com")
    protected String serverId;

    /**
     * The credential id (on <code>~/.aws/credentials</code> file) to use)
     */
    @Parameter(property = "beanstalker.credentialId", defaultValue = "default")
    protected String credentialId;

    /**
     * Verbose Logging?
     */
    @Parameter(property = "beanstalker.verbose", defaultValue = "false")
    protected boolean verbose;
    /**
     * Ignore Exceptions?
     */
    @Parameter(property = "beanstalker.ignoreExceptions", defaultValue = "false")
    protected boolean ignoreExceptions;
    protected String version = "?";
    protected Context context;
    /**
     * <p> Service region e.g. &quot;us-east-1&quot; </p>
     * <p/>
     * <p> <b>Note: Does not apply to all services.</b> </p>
     * <p/>
     * <p> <i>&quot;-Cloudfront, I'm talking to you! Look at me when I do that!&quot;</i> </p>
     * <p/>
     * <p> See <a href="http://docs.amazonwebservices.com/general/latest/gr/rande.html" >this list</a>
     * for reference. </p>
     * <p/>
     * <p>TODO Rationalize this</p>
     */
    @Parameter(property = "beanstalker.region")
    protected String regionName = "us-east-1";

    protected Region regionObj;

    protected Region getRegion() {
        if (null != regionObj) {
            return regionObj;
        }

        regionObj = RegionUtils.getRegion(regionName);

        Validate.notNull(regionObj, "Invalid region: " + regionName);

        return regionObj;
    }

    protected AWSClientFactory clientFactory;
    /**
     * Plexus container, needed to manually lookup components.
     * <p/>
     * To be able to use Password Encryption http://maven.apache.org/guides/mini/guide-encryption.html
     */
    private PlexusContainer container;
    private S service;

    protected AbstractAWSMojo() {
        setupVersion();
    }

    /**
     * Step through a sequence of prioritized credential providers using the first available type:
     * <ol>
     *     <li>{@link BasicAWSCredentials}</li>
     *     <li>{@link EnvironmentVariableCredentialsProvider}</li>
     *     <li>{@link ProfileCredentialsProvider}</li> (see {@link #getProfileEntry})
     *     <li>{@link InstanceProfileCredentialsProvider}</li>
     * </ol>
     * @return
     * @throws MojoFailureException
     */
    public AWSCredentialsProvider getAWSCredentials() throws MojoFailureException {
        if (null != this.awsCredentialsProvider) {
            return this.awsCredentialsProvider;
        }

        /*
         * Looks up on settings.xml for encrypted settings (the recommended way)
         */
        if (hasServerSettings()) {
            /*
             * This actually is the right way...
             */
            Expose expose = exposeSettings(serverId);

            String awsAccessKey = expose.getAccessKey();
            String awsSecretKey = expose.getSharedKey();

            this.awsCredentialsProvider =
                    new StaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        } else if (null != (awsCredentialsProvider = getEnvironmentKeys())) { // Attempts Environment
            // Already assigned. \o/
        } else if (null != (awsCredentialsProvider = getProfileEntry(credentialId))) { // Then Credential File (allows local testing)
            // meh
        } else if (null != (awsCredentialsProvider = getInstanceProfile())) { // Finally add IAM instance profile if present
            // still nothing
        } else {
            /*
             * Throws up. We have nowhere to get our credentials...
             */
            String errorMessage = "Entries in settings.xml for server "
                    + serverId
                    + " not defined. See http://docs.ingenieux.com.br/project/beanstalker/aws-config.html for more information";
            getLog().error(errorMessage);

            throw new MojoFailureException(errorMessage);
        }

        return this.awsCredentialsProvider;
    }

    private AWSCredentialsProvider getInstanceProfile() {
        return new InstanceProfileCredentialsProvider();
    }

    /**
     * Attempts to use the File Credentials Provider (~/.aws/credentials)
     *
     * @param credentialId Credential Id to use (default: "default")
     * @return credentials provider if successful, null otherwise
     */
    private AWSCredentialsProvider getProfileEntry(String credentialId) {
        try {
            final ProfileCredentialsProvider provider = new ProfileCredentialsProvider(defaultString(credentialId, "default"));

            provider.getCredentials();

            return provider;
        } catch (AmazonClientException exc) {
            return null;
        }
    }

    /**
     * Attempts to use Environment-based Provider Variables
     *
     * @return AWS Credentials Provider if available. Null otherwise.
     */
    private AWSCredentialsProvider getEnvironmentKeys() {
        final EnvironmentVariableCredentialsProvider
                provider =
                new EnvironmentVariableCredentialsProvider();

        try {
            provider.getCredentials();

            return provider;
        } catch (AmazonClientException exc) {
            return null;
        }
    }

    public AWSClientFactory getClientFactory() {
        return clientFactory;
    }

    protected Expose exposeSettings(String serverId) throws MojoFailureException {
        Server server = settings.getServer(serverId);

        Expose expose = new Expose();

        if (null != server) {
            expose.setServerId(serverId);
            expose.setAccessKey(server.getUsername());
            expose.setSharedKey(getDecryptedAwsKey(server.getPassword().trim()));
        } else {
            getLog().warn(format("serverId['%s'] not found. Using runtime defaults", serverId));

            expose.setServerId("runtime");
            expose.setAccessKey(getAWSCredentials().getCredentials().getAWSAccessKeyId());
            expose.setSharedKey(getAWSCredentials().getCredentials().getAWSSecretKey());
        }

        return expose;
    }

    /**
     * Decrypts (or warns) a supplied user password
     *
     * @param awsSecretKey Aws Secret Key
     * @return The same awsSecretKey - Decrypted
     */
    private String getDecryptedAwsKey(final String awsSecretKey) {
                /*
                 * Checks if we have a encrypted key. And warn if we don't.
		 */
        if (!(awsSecretKey.startsWith("{") && awsSecretKey.endsWith("}"))) {
            getLog().warn(
                    "You should encrypt your passwords. See http://beanstalker.ingenieux.com.br/security.html for more information");
        } else {
            /*
			 * ... but we do have a valid key. Lets decrypt and return it.
			 */
            return decryptPassword(awsSecretKey);
        }
        return awsSecretKey;
    }

    public void contextualize(final Context context) throws ContextException {
        this.context = context;
        this.container = (PlexusContainer) context
                .get(PlexusConstants.PLEXUS_KEY);
    }

    private boolean hasServerSettings() {
        if (serverId == null) {
            return false;
        } else {
            final Server srv = settings.getServer(serverId);
            return srv != null;
        }
    }

    private String decryptPassword(final String password) {
        if (password != null) {
            try {
                final Class<?> securityDispatcherClass = container.getClass()
                        .getClassLoader()
                        .loadClass(SECURITY_DISPATCHER_CLASS_NAME);
                final Object securityDispatcher = container.lookup(
                        SECURITY_DISPATCHER_CLASS_NAME, "maven");
                final Method decrypt = securityDispatcherClass.getMethod(
                        "decrypt", String.class);

                return (String) decrypt.invoke(securityDispatcher, password);
            } catch (final Exception e) {
                getLog().warn(
                        "security features are disabled. Cannot find plexus security dispatcher",
                        e);
            }
        }
        getLog().debug("password could not be decrypted");

        return password;
    }

    protected ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withUserAgent(getUserAgent());

        if (null != this.settings && null != settings.getActiveProxy()) {
            Proxy proxy = settings.getActiveProxy();

            clientConfiguration.setProxyHost(proxy.getHost());
            clientConfiguration.setProxyUsername(proxy.getUsername());
            clientConfiguration.setProxyPassword(proxy.getPassword());
            clientConfiguration.setProxyPort(proxy.getPort());
        }

        return clientConfiguration;
    }

    protected final String getUserAgent() {
        return
                format("Apache Maven/3.0 (ingenieux beanstalker/%s; http://beanstalker.ingenieux.com.br)",
                        version);
    }

    protected void setupVersion() {
        InputStream is = null;

        try {
            Properties properties = new Properties();

            is = getClass().getResourceAsStream("/beanstalker-core.properties");

            if (null != is) {
                properties.load(is);

                this.version = properties.getProperty("beanstalker.version");
            }
        } catch (Exception exc) {
            getLog().warn("Oops?", exc);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected void setupService() throws MojoExecutionException {
        @SuppressWarnings("unchecked")
        Class<S> serviceClass = (Class<S>) TypeUtil.getServiceClass(getClass());

        try {
            clientFactory = new AWSClientFactory(getAWSCredentials(), getClientConfiguration(),
                    regionName);

            this.service = clientFactory.getService(serviceClass);
        } catch (Exception exc) {
            throw new MojoExecutionException("Unable to create service", exc);
        }
    }

    public S getService() {
        if (null == service) {
            try {
                setupService();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return service;
    }

    @Override
    public final void execute() throws MojoExecutionException,
            MojoFailureException {
        Object result = null;

        try {

            configure();

            result = executeInternal();

            getLog().info("SUCCESS");
        } catch (Exception e) {
            getLog().warn("FAILURE", e);

            handleException(e);
            return;
        }

        displayResults(result);
    }

    /**
     * Extension Point - Meant for others to declare and redefine variables as needed.
     */
    protected void configure() {
    }

    public void handleException(Exception e) throws MojoExecutionException,
            MojoFailureException {
		/*
		 * This is actually the feature I really didn't want to have written,
		 * ever.
		 * 
		 * Thank you for reading this comment.
		 */
        if (ignoreExceptions) {
            getLog().warn(
                    "Ok. ignoreExceptions is set to true. No result for you!");

            return;
        } else if (MojoExecutionException.class.isAssignableFrom(e.getClass())) {
            throw (MojoExecutionException) e;
        } else if (MojoFailureException.class.isAssignableFrom(e.getClass())) {
            throw (MojoFailureException) e;
        } else {
            throw new MojoFailureException("Failed", e);
        }
    }

    protected void displayResults(Object result) {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            String resultAsJsonString = mapper.writeValueAsString(result);

            if ("null".equals(resultAsJsonString)) {
                getLog().info("null/void result");

                return;
            }

            List<String> lines = Arrays.asList(resultAsJsonString.split("\n"));

            for (String line : lines) {
                getLog().info(line);
            }
        } catch (Exception exc) {
            getLog().warn("Oops", exc);
        }
    }

    protected abstract Object executeInternal() throws Exception;

    public boolean isVerbose() {
        return verbose;
    }
}
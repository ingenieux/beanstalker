/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ingenieux.mojo.aws;

import com.google.common.base.Charsets;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.execution.MavenSession;
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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import br.com.ingenieux.mojo.aws.util.AWSClientFactory;
import br.com.ingenieux.mojo.aws.util.TypeUtil;

import static java.lang.String.format;
/**
 * Represents a Mojo which keeps AWS passwords <p/> TODO: Refactor into tiny, delegated classes.
 * Currently its a huge bloat, but it works, right? <p/> <p> <b>NOTE:</b> Settings in this class use
 * properties based in "beanstalker", which is the project. The beanstalk module, though, prefixes
 * then as "beanstalk" instead </p> <p/> Parts of this class come from <a
 * href="http://code.google.com/p/maven-gae-plugin">maven-gae-plugin</a>'s source code.
 */
public abstract class AbstractAWSMojo<S extends AmazonWebServiceClient> extends
        AbstractMojo
        implements Contextualizable {
    protected final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    @Parameter(defaultValue="${session}", required = true)
    protected MavenSession session;

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
     * <p> See <a href="http://docs.amazonwebservices.com/general/latest/gr/rande.html"
     * >this list</a> for reference. </p>
     */
    @Parameter(property = "beanstalker.region")
    protected String regionName = "us-east-1";

    protected Region regionObj;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public Region getRegion() {
        if (null != regionObj) {
            return regionObj;
        }

        regionObj = RegionUtils.getRegion(regionName);

        Validate.notNull(regionObj, "Invalid region: " + regionName);

        return regionObj;
    }

    protected AWSClientFactory clientFactory;
    /**
     * Plexus container, needed to manually lookup components. <p/> To be able to use Password
     * Encryption http://maven.apache.org/guides/mini/guide-encryption.html
     */
    private PlexusContainer container;
    private S service;

    protected AbstractAWSMojo() {
        setupVersion();

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //https://github.com/ingenieux/beanstalker/issues/87 - Decouple the serialization of AWS responses to avoid warning mgs
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    class BeanstalkerAWSCredentialsProviderChain extends AWSCredentialsProviderChain {
        public BeanstalkerAWSCredentialsProviderChain(String serverId, String profileName) {
            super(new ExposeCredentialsProvider(serverId),
                    new EnvironmentVariableCredentialsProvider(),
                    new SystemPropertiesCredentialsProvider(),
                    new ProfileCredentialsProvider(profileName),
                    new InstanceProfileCredentialsProvider());
        }
    }

    class ExposeCredentialsProvider implements AWSCredentialsProvider {
        private final String serverId;

        public ExposeCredentialsProvider(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public AWSCredentials getCredentials() {
            if (!hasServerSettings())
                return null;

            try {
                Expose expose = exposeSettings(serverId);

                String awsAccessKey = expose.getAccessKey();
                String awsSecretKey = expose.getSharedKey();

                return new StaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)).getCredentials();
            } catch (Exception exc) {
                throw new RuntimeException("Oops", exc);
            }
        }

        @Override
        public void refresh() {
        }
    }


    public AWSCredentialsProvider getAWSCredentials() throws MojoFailureException {
        if (null != this.awsCredentialsProvider) {
            return this.awsCredentialsProvider;
        }

        this.awsCredentialsProvider = new BeanstalkerAWSCredentialsProviderChain(serverId, credentialId);

        return awsCredentialsProvider;
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

        this.service = createServiceFor(serviceClass);
    }

    protected <T> T createServiceFor(Class<T> serviceClass) throws MojoExecutionException {
        try {
            clientFactory = new AWSClientFactory(getAWSCredentials(), getClientConfiguration(),
                    regionName);

            return clientFactory.getService(serviceClass);
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
        Object result;

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
        } else if (MojoExecutionException.class.isAssignableFrom(e.getClass())) {
            throw (MojoExecutionException) e;
        } else if (MojoFailureException.class.isAssignableFrom(e.getClass())) {
            throw (MojoFailureException) e;
        } else {
            throw new MojoFailureException("Failed", e);
        }
    }

    protected void displayResults(Object result) {
        try {
            String resultAsJsonString = objectMapper.writeValueAsString(result);

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
package br.com.ingenieux.mojo.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import br.com.ingenieux.mojo.aws.util.AWSClientFactory;
import br.com.ingenieux.mojo.aws.util.CredentialsUtil;
import br.com.ingenieux.mojo.aws.util.TypeUtil;

import static java.lang.String.format;

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
 * 
 * TODO: Refactor into tiny, delegated classes. Currently its a huge bloat, but
 * it works, right?
 * 
 * <p>
 * <b>NOTE:</b> Settings in this class use properties based in "beanstalker",
 * which is the project. The beanstalk module, though, prefixes then as
 * "beanstalk" instead
 * </p>
 * 
 * Parts of this class come from <a
 * href="http://code.google.com/p/maven-gae-plugin">maven-gae-plugin</a>'s
 * source code.
 */
public abstract class AbstractAWSMojo<S extends AmazonWebServiceClient> extends
		AbstractMojo implements Contextualizable {
	private static final String SECURITY_DISPATCHER_CLASS_NAME = "org.sonatype.plexus.components.sec.dispatcher.SecDispatcher";

	/**
	 * Plexus container, needed to manually lookup components.
	 * 
	 * To be able to use Password Encryption
	 * http://maven.apache.org/guides/mini/guide-encryption.html
	 */
	private PlexusContainer container;

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
	 * The server id in maven settings.xml to use for AWS Services Credentials
	 * (accessKey / secretKey)
	 */
	@Parameter(property = "beanstalker.serverId", defaultValue = "aws.amazon.com")
	protected String serverId;

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

	public AWSCredentialsProvider getAWSCredentials() throws MojoFailureException {
        if (null != this.awsCredentialsProvider)
            return this.awsCredentialsProvider;

        /*
         * Are you using aws.accessKey and aws.secretKey? j'accuse!
         */
        if (hasServerSettings()) {
            /*
             * This actually is the right way...
             */
            Expose expose = exposeSettings(serverId);

            String awsAccessKey = expose.getAccessKey();
            String awsSecretKey = expose.getSharedKey();

            this.awsCredentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        } else if (null != (awsCredentialsProvider = getEnvironmentKeys())) {
            // Already assigned. \o/
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

    /**
     * Attempts to use Environment-based Provider Variables
     *
     * @return AWS Credentials Provider if available. Null otherwise.
     */
    private AWSCredentialsProvider getEnvironmentKeys() {
        final EnvironmentVariableCredentialsProvider provider = new EnvironmentVariableCredentialsProvider();

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
	 * @param awsSecretKey
	 *            Aws Secret Key
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

	/**
	 * <p>
	 * Service region e.g. &quot;us-east-1&quot;
	 * </p>
	 * 
	 * <p>
	 * <b>Note: Does not apply to all services.</b>
	 * </p>
	 * 
	 * <p>
	 * <i>&quot;-Cloudfront, I'm talking to you! Look at me when I do
	 * that!&quot;</i>
	 * </p>
	 * 
	 * <p>
	 * See <a
	 * href="http://docs.amazonwebservices.com/general/latest/gr/rande.html"
	 * >this list</a> for reference.
	 * </p>
	 */
	@Parameter(property = "beanstalker.region")
	protected String region;

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

		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	protected AbstractAWSMojo() {
		setupVersion();
	}

	protected void setupService() throws MojoExecutionException {
		@SuppressWarnings("unchecked")
		Class<S> serviceClass = (Class<S>) TypeUtil.getServiceClass(getClass());

		try {
			clientFactory = new AWSClientFactory(getAWSCredentials(), getClientConfiguration(), region);
			
			this.service = clientFactory.getService(serviceClass);
		} catch (Exception exc) {
			throw new MojoExecutionException("Unable to create service", exc);
		}
	}

	private S service;

	protected AWSClientFactory clientFactory;
	
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

		displayResults(result, 0);
	}

	/**
	 * Extension Point - Meant for others to declare and redefine variables as
	 * needed.
	 * 
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

	protected void displayResults(Object result, int level) {
		if (null == result)
			return;
		
		String prefix = StringUtils.repeat(" ", level * 2) + " * ";

		if (Collection.class.isAssignableFrom(result.getClass())) {
			@SuppressWarnings("unchecked")
			Collection<Object> coll = Collection.class.cast(result);
			
			for (Object o : coll)
				displayResults(o, 1 + level);
			
			return;
		} else if ("java.lang".equals(result.getClass().getPackage().getName())) {
			getLog().info(prefix + CredentialsUtil.redact("" + result) + " [class: "
					+ result.getClass().getSimpleName() + "]");
			
			return;
		}

		BeanMap beanMap = new BeanMap(result);

		for (Iterator<?> itProperty = beanMap.keyIterator(); itProperty
				.hasNext();) {
			String propertyName = "" + itProperty.next();
			Object propertyValue = beanMap.get(propertyName);

			if ("class".equals(propertyName))
				continue;

			if (null == propertyValue)
				continue;

			Class<?> propertyClass = null;

			try {
				propertyClass = beanMap.getType(propertyName);
			} catch (Exception e) {
				getLog().warn("Failure on property " + propertyName, e);
			}
			
			if (null == propertyClass) {
				getLog().info(prefix + propertyName + ": " + CredentialsUtil.redact("" + propertyValue));
			} else {
				getLog().info(prefix + 
						propertyName + ": " + CredentialsUtil.redact("" + propertyValue) + " [class: "
								+ propertyClass.getSimpleName() + "]");
			}
		}
	}

	protected abstract Object executeInternal() throws Exception;

	public boolean isVerbose() {
		return verbose;
	}
}
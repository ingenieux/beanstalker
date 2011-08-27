package br.com.ingenieux.mojo.beanstalk;

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

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
 * Parts of this class come from <a
 * href="http://code.google.com/p/maven-gae-plugin">maven-gae-plugin</a>'s
 * source code.
 */
public abstract class AbstractAWSMojo extends AbstractMojo implements
    Contextualizable {
	private static final String SECURITY_DISPATCHER_CLASS_NAME = "org.sonatype.plexus.components.sec.dispatcher.SecDispatcher";

	/**
	 * Plexus container, needed to manually lookup components.
	 * 
	 * To be able to use Password Encryption
	 * http://maven.apache.org/guides/mini/guide-encryption.html
	 */
	private PlexusContainer container;

	/**
	 * The Maven settings reference.
	 * 
	 * @parameter expression="${settings}"
	 * @required
	 * @readonly
	 */
	private Settings settings;

	/**
	 * AWS Access Key
	 * 
	 * @parameter expression="${aws.accessKey}"
	 */
	protected String accessKey;

	/**
	 * AWS Secret Key
	 * 
	 * @parameter expression="${aws.secretKey}"
	 */
	protected String secretKey;

	/**
	 * AWS Credentials
	 */
	protected AWSCredentials awsCredentials;

	/**
	 * The server id in maven settings.xml to use for AWS Services Credentials
	 * (accessKey / secretKey)
	 * 
	 * If password present in settings "--passin" is set automatically.
	 * 
	 * @parameter expression="${beanstalk.serverId}"
	 *            default-value="aws.amazon.com"
	 */
	protected String serverId;

	public AWSCredentials getAWSCredentials() throws MojoFailureException {
		/*
		 * Construct if needed
		 */
		if (null == this.awsCredentials) {
			String awsSecretKey = null;
			String awsAccessKey = null;

			/*
			 * Are you using aws.accessKey and aws.secretKey? j'accuser!
			 */
			if (StringUtils.isNotBlank(accessKey)
			    || StringUtils.isNotBlank(secretKey)) {
				getLog()
				    .warn(
				        "Warning! Usage of accessKey and secretKey is being "
				            + "deprecated! "
				            + "See http://beanstalker.ingenieux.com.br/usage.html for more information");
				awsAccessKey = accessKey;
				awsSecretKey = getDecryptedAwsKey(secretKey);
			} else if (hasServerSettings()) {
				/*
				 * This actually is the right way...
				 */
				Server server = settings.getServer(serverId);

				awsAccessKey = server.getUsername();
				awsSecretKey = getDecryptedAwsKey(server.getPassword().trim());
			} else {
				/*
				 * Throws up. We have nowhere to get our credentials...
				 */
				String errorMessage = "Entries in settings.xml for server "
				    + serverId
				    + " not defined. See http://beanstalker.ingenieux.com.br/usage.html for more information";
				getLog().error(errorMessage);

				throw new MojoFailureException(errorMessage);
			}

			this.awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		}

		return this.awsCredentials;
	}

	/**
	 * Decrypts (or warns) a supplied user password
	 * 
	 * @param awsSecretKey
	 *          Aws Secret Key
	 * @return The same awsSecretKey - Decrypted
	 */
	private String getDecryptedAwsKey(final String awsSecretKey) {
		/*
		 * Checks if we have a encrypted key. And warn if we don't.
		 */
		if (!(awsSecretKey.startsWith("{") && awsSecretKey.endsWith("}"))) {
			getLog()
			    .warn(
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
		this.container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
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
				    .getClassLoader().loadClass(SECURITY_DISPATCHER_CLASS_NAME);
				final Object securityDispatcher = container.lookup(
				    SECURITY_DISPATCHER_CLASS_NAME, "maven");
				final Method decrypt = securityDispatcherClass.getMethod("decrypt",
				    String.class);

				return (String) decrypt.invoke(securityDispatcher, password);

			} catch (final Exception e) {
				getLog()
				    .warn(
				        "security features are disabled. Cannot find plexus security dispatcher",
				        e);
			}
		}
		getLog().debug("password could not be decrypted");

		return password;
	}
}
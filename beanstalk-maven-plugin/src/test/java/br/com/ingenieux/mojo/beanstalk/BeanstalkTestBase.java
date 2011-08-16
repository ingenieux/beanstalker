package br.com.ingenieux.mojo.beanstalk;

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
import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import com.amazonaws.auth.AWSCredentials;

public abstract class BeanstalkTestBase extends AbstractMojoTestCase {
	Properties properties;
	
	StrSubstitutor strSub;

	CheckAvailabilityMojo checkAvailabilityMojo;

	CreateApplicationVersionMojo createAppVersionMojo;

	UploadSourceBundleMojo uploadSourceBundleMojo;

	CreateEnvironmentMojo createEnvMojo;

	TerminateEnvironmentMojo termEnvMojo;

	WaitForEnvironmentMojo waitForEnvMojo;

	UpdateEnvironmentMojo updateEnvMojo;
	
	DescribeConfigurationTemplatesMojo describeConfigTemplatesMojo;

	String versionLabel;
	
	AWSCredentials credentials;

	public BeanstalkTestBase() {
		super();
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Properties properties = new Properties();
		
		properties.load(new FileInputStream("test.properties"));
		
		this.properties = properties;

		uploadSourceBundleMojo = getMojo(UploadSourceBundleMojo.class);

		createAppVersionMojo = getMojo(CreateApplicationVersionMojo.class);

		createEnvMojo = getMojo(CreateEnvironmentMojo.class);

		waitForEnvMojo = getMojo(WaitForEnvironmentMojo.class);

		termEnvMojo = getMojo(TerminateEnvironmentMojo.class);

		updateEnvMojo = getMojo(UpdateEnvironmentMojo.class);
		
		describeConfigTemplatesMojo = getMojo(DescribeConfigurationTemplatesMojo.class);

		versionLabel = String.format("test-%08X", System.currentTimeMillis());
		
		checkAvailabilityMojo = getMojo(CheckAvailabilityMojo.class);
		
		this.credentials = uploadSourceBundleMojo.getAWSCredentials();
	}

	protected File getBasePom() {
    return new File(getBasedir(),
  	    "target/test-classes/br/com/ingenieux/mojo/beanstalk/pom.xml");
  }

	@SuppressWarnings("unchecked")
	protected <T extends AbstractBeanstalkMojo> T getMojo(Class<T> mojoClazz)
      throws Exception {
      	File testPom = this.getBasePom();
      
      	PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
      	    "beanstalk-maven-plugin", testPom);
      
      	return (T) configureMojo(mojoClazz.newInstance(), pluginConfiguration);
      }

	protected File getWarFile() throws URISyntaxException {
  	return new File(BeanstalkTestBase.class.getResource("test-war.war").toURI());
  }

	protected String getS3Path() {
  	properties.put("versionLabel", this.versionLabel);
  
  	strSub = new StrSubstitutor(properties);
  	return strSub.replace(properties.get("s3KeyMask"));
  }

	protected String getS3Bucket() {
  	return properties.getProperty("s3Bucket");
  }

}
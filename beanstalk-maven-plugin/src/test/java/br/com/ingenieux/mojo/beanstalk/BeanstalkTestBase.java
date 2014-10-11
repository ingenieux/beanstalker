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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Ignore;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;
import br.com.ingenieux.mojo.beanstalk.app.CreateApplicationMojo;
import br.com.ingenieux.mojo.beanstalk.bundle.UploadSourceBundleMojo;
import br.com.ingenieux.mojo.beanstalk.config.CreateConfigurationTemplateMojo;
import br.com.ingenieux.mojo.beanstalk.config.DescribeConfigurationTemplatesMojo;
import br.com.ingenieux.mojo.beanstalk.dns.CheckAvailabilityMojo;
import br.com.ingenieux.mojo.beanstalk.env.CreateEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.env.TerminateEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.env.UpdateEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.env.WaitForEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.version.CreateApplicationVersionMojo;

@Ignore
public abstract class BeanstalkTestBase extends AbstractMojoTestCase {

  public static final String PROP_VERSION_LABEL = "versionLabel";

  public static final String PROP_S3_KEY_MASK = "s3KeyMask";

  public static final String PROP_S3_BUCKET = "s3Bucket";

  Properties properties;

  StrSubstitutor strSub;

  CheckAvailabilityMojo checkAvailabilityMojo;

  CreateApplicationMojo createAppMojo;

  CreateApplicationVersionMojo createAppVersionMojo;

  UploadSourceBundleMojo uploadSourceBundleMojo;

  CreateEnvironmentMojo createEnvMojo;

  TerminateEnvironmentMojo termEnvMojo;

  WaitForEnvironmentMojo waitForEnvMojo;

  UpdateEnvironmentMojo updateEnvMojo;

  DescribeConfigurationTemplatesMojo describeConfigTemplatesMojo;

  CreateConfigurationTemplateMojo createConfigurationTemplateMojo;

  String versionLabel;

  AWSCredentials credentials;

  AWSElasticBeanstalk service;

  public BeanstalkTestBase() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Properties properties = new Properties();

    properties.load(new FileInputStream("test.properties"));

    this.properties = properties;

    configureMojos();

    this.credentials = uploadSourceBundleMojo.getAWSCredentials().getCredentials();

    this.service = new AWSElasticBeanstalkClient(credentials);
  }

  protected void configureMojos() throws Exception {
    createAppMojo = getMojo(CreateApplicationMojo.class);

    createAppVersionMojo = getMojo(CreateApplicationVersionMojo.class);

    uploadSourceBundleMojo = getMojo(UploadSourceBundleMojo.class);

    createAppMojo = getMojo(CreateApplicationMojo.class);

    createAppVersionMojo = getMojo(CreateApplicationVersionMojo.class);

    createEnvMojo = getMojo(CreateEnvironmentMojo.class);

    waitForEnvMojo = getMojo(WaitForEnvironmentMojo.class);

    termEnvMojo = getMojo(TerminateEnvironmentMojo.class);

    updateEnvMojo = getMojo(UpdateEnvironmentMojo.class);

    describeConfigTemplatesMojo = getMojo(DescribeConfigurationTemplatesMojo.class);

    versionLabel = String.format("test-%08X", System.currentTimeMillis());

    checkAvailabilityMojo = getMojo(CheckAvailabilityMojo.class);

    createConfigurationTemplateMojo = getMojo(CreateConfigurationTemplateMojo.class);
  }

  protected File getBasePom(String pomName) {
    return new File(getBasedir(),
                    "target/test-classes/br/com/ingenieux/mojo/beanstalk/" + pomName);
  }

  @SuppressWarnings("unchecked")
  protected <T extends AbstractAWSMojo<?>> T getMojo(Class<T> mojoClazz)
      throws Exception {
    File testPom = this.getBasePom("pom.xml");

    PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
        "beanstalk-maven-plugin", testPom);

    return (T) configureMojo(mojoClazz.newInstance(), pluginConfiguration);
  }

  protected File getWarFile() throws URISyntaxException {
    return new File(BeanstalkTestBase.class.getResource("test-war.war").toURI());
  }

  protected String getS3Path() {
    properties.put(PROP_VERSION_LABEL, this.versionLabel);

    strSub = new StrSubstitutor(properties);
    return strSub.replace(properties.get(PROP_S3_KEY_MASK));
  }

  protected String getS3Bucket() {
    return properties.getProperty(PROP_S3_BUCKET);
  }

  public void clearEnvironments() {
    DescribeEnvironmentsResult environments = service.describeEnvironments();

    for (EnvironmentDescription d : environments.getEnvironments()) {
      service
          .terminateEnvironment(new TerminateEnvironmentRequest()
                                    .withEnvironmentId(d.getEnvironmentId()).withTerminateResources(
                  true));
    }
  }

}
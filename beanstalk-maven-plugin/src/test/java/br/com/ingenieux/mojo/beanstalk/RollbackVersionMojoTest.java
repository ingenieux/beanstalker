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

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Ignore;

import java.io.File;

import br.com.ingenieux.mojo.beanstalk.version.RollbackVersionMojo;

@Ignore
public class RollbackVersionMojoTest extends BeanstalkTestBase {

  private RollbackVersionMojo mojo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    File testPom = super.getBasePom("pom.xml");

    PlexusConfiguration pluginConfiguration = extractPluginConfiguration(
        "beanstalk-maven-plugin", testPom);

    RollbackVersionMojo mojo = (RollbackVersionMojo) configureMojo(
        new RollbackVersionMojo(), pluginConfiguration);

    this.mojo = mojo;
  }

  public void ignoretestPreviousVersion() throws Exception {
    setVariableValueToObject(mojo, "applicationName", "belemtransito");
    setVariableValueToObject(mojo, "environmentName", "production");

    mojo.execute();
  }

  public void ignoretestLatestInstead() throws Exception {
    setVariableValueToObject(mojo, "applicationName", "belemtransito");
    setVariableValueToObject(mojo, "environmentName", "production");
    setVariableValueToObject(mojo, "latestVersionInstead", true);

    mojo.execute();
  }
}

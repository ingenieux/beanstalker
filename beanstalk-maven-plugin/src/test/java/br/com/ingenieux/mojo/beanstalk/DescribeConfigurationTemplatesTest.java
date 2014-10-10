package br.com.ingenieux.mojo.beanstalk;

import org.junit.Ignore;

import br.com.ingenieux.mojo.beanstalk.app.CreateApplicationMojo;
import br.com.ingenieux.mojo.beanstalk.version.CreateApplicationVersionMojo;


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

@Ignore
public class DescribeConfigurationTemplatesTest extends BeanstalkTestBase {

  @Override
  protected void configureMojos() throws Exception {
    createAppMojo = getMojo(CreateApplicationMojo.class);

    createAppVersionMojo = getMojo(CreateApplicationVersionMojo.class);
  }

  public void testDescribeConfigurationTemplates() throws Exception {
    String appName = properties.getProperty("appname");

    setVariableValueToObject(createAppMojo, "applicationName", appName);
    setVariableValueToObject(createConfigurationTemplateMojo, "applicationName", appName);

    createAppMojo.execute();

    createConfigurationTemplateMojo.execute();
  }
}

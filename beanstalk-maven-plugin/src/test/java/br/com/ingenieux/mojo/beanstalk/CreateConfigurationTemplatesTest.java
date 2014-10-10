package br.com.ingenieux.mojo.beanstalk;

import org.junit.Ignore;

import java.io.File;

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
public class CreateConfigurationTemplatesTest extends BeanstalkTestBase {

  public void testDescribeConfigurationTemplates() throws Exception {
    setVariableValueToObject(describeConfigTemplatesMojo, "applicationName",
                             "belemtransito");

    describeConfigTemplatesMojo.execute();
  }

  public void testDescribeConfigurationTemplatesToFile() throws Exception {
    File outputFile = new File("config-template.test");

    outputFile.delete();

    assertFalse(outputFile.exists());

    setVariableValueToObject(describeConfigTemplatesMojo, "outputFile",
                             outputFile);

    setVariableValueToObject(describeConfigTemplatesMojo, "applicationName",
                             "belemtransito");

    describeConfigTemplatesMojo.execute();

    assertTrue(outputFile.exists());
  }
}

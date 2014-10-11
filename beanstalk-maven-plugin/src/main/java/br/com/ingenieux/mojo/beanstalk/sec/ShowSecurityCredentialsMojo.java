package br.com.ingenieux.mojo.beanstalk.sec;

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

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import br.com.ingenieux.mojo.aws.AbstractAWSMojo;

/**
 * <p>Shows the IAM security credentials from settings.xml into project properties</p>
 *
 * <p>See <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/identitymanagement/AmazonIdentityManagement.html#getUser()">for
 * more information</a></p>
 *
 * @since 0.2.9-SNAPSHOT
 */
@Mojo(name = "show-security-credentials")
public class ShowSecurityCredentialsMojo extends AbstractAWSMojo<AmazonIdentityManagementClient> {

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    return getService().getUser();
  }

}

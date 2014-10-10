package br.com.ingenieux.mojo.beanstalk.dns;

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

import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Checks the availability of a CNAME.
 *
 * See the <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_CheckDNSAvailability.html"
 * >CheckDNSAvailability API</a> call.
 *
 * @since 0.1.0
 */
@Mojo(name = "check-availability", requiresDirectInvocation = true)
public class CheckAvailabilityMojo extends AbstractBeanstalkMojo {

  /**
   * DNS CName Prefix
   */
  @Parameter(property = "beanstalk.cnamePrefix", required = true)
  String cnamePrefix;

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    CheckDNSAvailabilityRequest checkDNSAvailabilityRequest = new CheckDNSAvailabilityRequest(
        cnamePrefix);

    CheckDNSAvailabilityResult result = getService()
        .checkDNSAvailability(checkDNSAvailabilityRequest);

    return result;
  }
}

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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsCommand;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsContext;
import br.com.ingenieux.mojo.beanstalk.cmd.dns.BindDomainsContextBuilder;

import static java.util.Arrays.asList;

/**
 * <p> Binds an Elastic Beanstalk Environment into a set of Route53 records </p>
 *
 * <p> NOTE: THIS IS HIGHLY EXPERIMENTAL CODE </p>
 *
 * @since 0.2.9
 */
@Mojo(name = "bind-domains")
public class BindDomainsMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * <p>List of Domains</p>
   *
   * <p>Could be set as either:</p> <ul> <li>fqdn:hostedZoneId (e.g. "services.modafocas.org:Z3DJ4DL0DIEEJA")</li>
   * <li>hosted zone name - will be set to root. (e.g., "modafocas.org")</li> </ul>
   */
  @Parameter(property = "beanstalk.domains")
  String[] domains;

  @Override
  protected Object executeInternal() throws Exception {
    final BindDomainsContext
        ctx =
        new BindDomainsContextBuilder().withCurEnv(this.curEnv).withDomains(asList(domains))
            .build();

    new BindDomainsCommand(this).execute(
        ctx);

    return null;
  }
}

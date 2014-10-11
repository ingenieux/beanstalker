// CHECKSTYLE:OFF

package br.com.ingenieux.mojo.beanstalk.cmd.dns;

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

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

import java.util.Collection;

public class BindDomainsContextBuilder extends
                                       BindDomainsContextBuilderBase<BindDomainsContextBuilder> {

  public BindDomainsContextBuilder() {
    super(new BindDomainsContext());
  }

  public static BindDomainsContextBuilder createBindDomainsContext() {
    return new BindDomainsContextBuilder();
  }

  public BindDomainsContext build() {
    return getInstance();
  }
}

class BindDomainsContextBuilderBase<GeneratorT extends BindDomainsContextBuilderBase<GeneratorT>> {

  private BindDomainsContext instance;

  protected BindDomainsContextBuilderBase(
      BindDomainsContext aInstance) {
    instance = aInstance;
  }

  protected BindDomainsContext getInstance() {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withCurEnv(EnvironmentDescription env) {
    instance.curEnv = env;

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withDomains(Collection<String> domains) {
    instance.domains = domains;

    return (GeneratorT) this;
  }
}

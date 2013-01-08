package br.com.ingenieux.mojo.simpledb;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;

import com.amazonaws.services.simpledb.model.DeleteDomainRequest;

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
 * Goal which deletes (drops) domains
 * 
 */
@Mojo(name="delete-domains", requiresDirectInvocation=true)
public class DeleteDomainsMojo extends AbstractSimpleDbDomainMojo {
    @Override
	protected void configure() {
        if ("*".equals(this.domains))
			this.domains = StringUtils.join(getService().listDomains()
					.getDomainNames(), ",");

        super.configure();
    }

    @Override
    protected Object executeInternal() throws Exception {
        if (getLog().isInfoEnabled())
            getLog().info("Deleting Domains: " + StringUtils.join(domainsCollection, ", "));

        for (String domain : domainsCollection) {
            if (getLog().isInfoEnabled())
                getLog().info(" * " + domain);

			getService().deleteDomain(new DeleteDomainRequest(domain));
        }

        return null;
    }
}

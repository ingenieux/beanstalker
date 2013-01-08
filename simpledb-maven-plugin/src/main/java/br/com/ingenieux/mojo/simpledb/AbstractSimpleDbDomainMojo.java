package br.com.ingenieux.mojo.simpledb;

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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractSimpleDbDomainMojo extends AbstractSimpleDbMojo {
    public static final Pattern PATTERN_DOMAIN = Pattern.compile("^[-a-z0-9._]{3,255}$", Pattern.CASE_INSENSITIVE);
    

    /**
     * Domains to Create, comma-separated
     */
	@Parameter(property="simpledb.domains", required = true)
    String domains;
    
    /**
     * Collection of Domains to Create
     */
    protected Collection<String> domainsCollection = new TreeSet<String>();

    public AbstractSimpleDbDomainMojo() {
        super();
    }

    @Override
	protected void configure() {
        validate("domains should not blank", isNotBlank(domains));
    
        domainsCollection.addAll(Arrays.asList(domains.split("\\,+")));
    
        for (String newDomainToCreate : domainsCollection)
            validate(String.format("Invalid Domain Name: %s", newDomainToCreate),
                    PATTERN_DOMAIN.matcher(newDomainToCreate).matches());
    }

}
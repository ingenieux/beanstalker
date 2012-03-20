package br.com.ingenieux.mojo.simpledb.cmd;

import java.io.Reader;

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

public class PutAttributesContext {
    final String domain;

    final Reader source;
    
    final boolean createDomainIfNeeded;

    public PutAttributesContext(String domain, Reader source, boolean createDomainIfNeeded) {
        super();
        this.domain = domain;
        this.source = source;
        this.createDomainIfNeeded = createDomainIfNeeded;
    }

    public String getDomain() {
        return domain;
    }

    public Reader getSource() {
        return source;
    }

    public boolean isCreateDomainIfNeeded() {
        return createDomainIfNeeded;
    }
    
    

}

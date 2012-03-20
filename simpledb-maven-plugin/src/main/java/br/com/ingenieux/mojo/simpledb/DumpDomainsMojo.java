package br.com.ingenieux.mojo.simpledb;

import java.io.File;

import org.jfrog.maven.annomojo.annotations.MojoGoal;

import br.com.ingenieux.mojo.simpledb.cmd.DumpDomainCommand;
import br.com.ingenieux.mojo.simpledb.cmd.DumpDomainContext;

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
 * Goal which creates domains
 * 
 */
@MojoGoal("dump-domains")
public class DumpDomainsMojo extends AbstractSimpleDbDomainMojo {
    @Override
    protected Object executeInternal() throws Exception {
        for (String domainToDump : domainsCollection) {
            File outputFile = new File(String.format("target/%s.json", domainToDump));
            
			new DumpDomainCommand(this.getService())
					.execute(new DumpDomainContext(outputFile, domainToDump));
        }
        
        return null;
    }
}

package br.com.ingenieux.mojo.simpledb;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileReader;

import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

import br.com.ingenieux.mojo.simpledb.cmd.PutAttributesCommand;
import br.com.ingenieux.mojo.simpledb.cmd.PutAttributesContext;

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
 */
@MojoGoal("put-attributes")
public class PutAttributesMojo extends AbstractSimpleDbMojo {
    /**
     * Relation of Attributes to Put, comma-separated
     */
	@MojoParameter(expression = "${simpledb.file}", required = true)
    private File file;

    /**
     * Name of Domain to Put
     */
	@MojoParameter(expression = "${simpledb.domain}", required = true)
    private String domain;

    @Override
	protected void configure() {
        validate("domain is blank", isNotBlank(domain));
    }

    @Override
    protected Object executeInternal() throws Exception {
        PutAttributesContext ctx = new PutAttributesContext(domain, new FileReader(file), true);
        
		new PutAttributesCommand(getService()).execute(ctx);

        return null;
    }
}

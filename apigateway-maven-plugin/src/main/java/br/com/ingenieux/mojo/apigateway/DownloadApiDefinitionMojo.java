/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package br.com.ingenieux.mojo.apigateway;

import com.amazonaws.services.apigateway.model.GetExportRequest;
import com.amazonaws.services.apigateway.model.GetExportResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

@Mojo(name = "download-api-definition", requiresProject = true)
public class DownloadApiDefinitionMojo extends AbstractAPIGatewayMojo {
    /**
     * Output File
     */
    @Parameter(property = "apigateway.outputFile", defaultValue = "${project.build.outputDirectory}/apigateway-swagger.json")
    File outputFile;

    @Override
    protected Object executeInternal() throws Exception {
        this.lookupIds();

        Validate.notNull(restApiId);
        Validate.notNull(stageName);

        Map<String, String> parameters = new LinkedHashMap<>();

        parameters.put("extensions", "integrations,authorizers,postman");

        final GetExportResult swaggerApi = getService().getExport(
                new GetExportRequest()
                        .withExportType("swagger")
                        .withAccepts("application/json")
                        .withRestApiId(restApiId)
                        .withStageName(stageName)
                        .withParameters(parameters)
        );

        String content = new String(swaggerApi.getBody().array(), Charset.defaultCharset());

        getLog().info("Content: " + content);

        if (null != outputFile) {
            if (!outputFile.exists()) {
                outputFile.getParentFile().mkdirs();

                getLog().info("Writing into file " + outputFile.getPath());

                IOUtils.write(content, new FileOutputStream(outputFile));
            }
        }

        return null;
    }
}

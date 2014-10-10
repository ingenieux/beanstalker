package br.com.ingenieux.mojo.beanstalk.env;

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

import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateResult;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * <p>Tags an Environment</p>
 *
 * <p>Defaults to environmentRef-yyyyMMdd-nn, where 'nn' is incremented according to
 * availability.</p>
 *
 * @since 1.1.0
 */
@Mojo(name = "tag-environment", requiresDirectInvocation = true)
public class TagEnvironmentMojo extends AbstractNeedsEnvironmentMojo {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

  /**
   * Template Name to use
   */
  @Parameter(property = "beanstalk.templateName")
  String templateName;

  @Override
  protected Object executeInternal() throws AbstractMojoExecutionException {
    Set<String> configTemplates = new TreeSet<String>(
        super.getConfigurationTemplates(applicationName));
    String today = DATE_FORMAT.format(new Date());

    if (StringUtils.isBlank(templateName)) {
      int i = 1;

      do {
        templateName = String.format("%s-%s-%02d", curEnv.getEnvironmentName(), today, i++);
      } while (configTemplates.contains(templateName));
    }

    CreateConfigurationTemplateResult result = getService().createConfigurationTemplate(
        new CreateConfigurationTemplateRequest().withEnvironmentId(
            curEnv.getEnvironmentId()).withTemplateName(
            templateName).withApplicationName(curEnv.getApplicationName()));

    getLog().info("Created config template " + templateName + " for environment " + curEnv
        .getEnvironmentId());

    return result;
  }
}

package br.com.ingenieux.mojo.beanstalk.sec;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import br.com.ingenieux.mojo.aws.Expose;
import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

/**
 * Exposes (i.e., copies) the security credentials from settings.xml into project properties
 *
 * <p> You can define the server, or not. If you don't, it will work if you did something like that
 * </p>
 *
 * <pre>
 * &lt;configuration&gt;
 * &nbsp;&nbsp;&lt;exposes&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;expose&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;serverId&gt;${beanstalk.serverId}&lt;/serverId&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;accessKey&gt;aws.accessKey&lt;/accessKey&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;secretKey&gt;aws.accessKey&lt;/secretKey&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/expose&gt;
 * &nbsp;&nbsp;&lt;/exposes&gt;
 * &lt;/configuration&gt;
 * </pre>
 *
 * <p> While it might look silly (and silly enough to get its own Plugin instead of beanstalker), it
 * power comes when combined with the <a href="http://mojo.codehaus.org/properties-maven-plugin/">Properties
 * Maven Plugin</a> </p>
 *
 * @since 0.2.7-RC4
 */
@Mojo(name = "expose-security-credentials", requiresProject = true)
public class ExposeSecurityCredentialsMojo extends AbstractBeanstalkMojo {

  /**
   * Which Server Settings to Expose?
   */
  @Parameter
  Expose[] exposes = new Expose[0];

  @Parameter(defaultValue = "${project}")
  MavenProject project;
  /**
   * @component
   */
  BuildContext buildContext;

  protected Object executeInternal() throws MojoExecutionException,
                                            MojoFailureException {
    /**
     * Fill in defaults if needed
     */
    if (0 == exposes.length) {
      exposes = new Expose[1];
      exposes[0] = new Expose();
      exposes[0].setServerId(this.serverId);
      exposes[0].setAccessKey("aws.accessKey");
      exposes[0].setSharedKey("aws.secretKey");
    } else {
      /**
       * Validate parameters, for gods sake
       */
      try {
        for (Expose e : exposes) {
          assertOrWarn(StringUtils.isNotBlank(e.getServerId()),
                       "serverId must be supplied");
          assertOrWarn(StringUtils.isNotBlank(e.getAccessKey()),
                       "accessKey must be supplied");
          assertOrWarn(StringUtils.isNotBlank(e.getSharedKey()),
                       "sharedKey must be supplied");
        }
      } catch (IllegalStateException e) {
        return null;
      }
    }

    for (Expose e : exposes) {
      Expose realExpose = null;

      try {
        realExpose = super.exposeSettings(e.getServerId());
      } catch (Exception exc) {
        getLog().warn("Failed to Expose Settings from serverId ('" + e.getServerId() + "')");
        continue;
      }

      getLog().info(
          String.format(
              "Writing Security Settings from serverId ('%s') into properties '%s' (accessKey) and '%s' (secretKey)",
              e.getServerId(), e.getAccessKey(), e.getSharedKey()));

      project.getProperties().put(e.getAccessKey(),
                                  realExpose.getAccessKey());

      project.getProperties().put(e.getSharedKey(),
                                  realExpose.getSharedKey());
    }

    return null;
  }

  private void assertOrWarn(boolean condition, String message) {
    if (condition) {
      return;
    }

    if (null != buildContext) {
      buildContext.addMessage(project.getFile(), 1, 1, message,
                              BuildContext.SEVERITY_WARNING, null);
    } else {
      getLog().warn(message);
    }

    throw new IllegalStateException(message);
  }
}

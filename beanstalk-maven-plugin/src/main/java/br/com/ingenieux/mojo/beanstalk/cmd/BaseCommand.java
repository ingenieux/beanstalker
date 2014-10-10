package br.com.ingenieux.mojo.beanstalk.cmd;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import br.com.ingenieux.mojo.beanstalk.AbstractBeanstalkMojo;

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
public abstract class BaseCommand<I, O> implements Command<I, O> {

  /**
   * Logger
   */
  protected Log logger;

  /**
   * Service
   */
  protected AWSElasticBeanstalkClient service;

  /**
   * Parent Mojo
   */
  protected AbstractBeanstalkMojo parentMojo;

  /**
   * Constructor
   *
   * @param parentMojo parent mojo
   */
  protected BaseCommand(AbstractBeanstalkMojo parentMojo) throws MojoExecutionException {
    this.parentMojo = parentMojo;

    this.service = parentMojo.getService();

    this.logger = parentMojo.getLog();
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public void debug(CharSequence message, Object... args) {
    logger.debug(String.format("" + message, args));
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public void info(CharSequence message, Object... args) {
    logger.info(String.format("" + message, args));
  }

  public final O execute(I context) throws MojoFailureException,
                                           MojoExecutionException {
    try {
      return executeInternal(context);
    } catch (Exception exc) {
      handleException(exc);

      throw new RuntimeException("Unlikely");
    }
  }

  private void handleException(Exception exc) throws MojoExecutionException,
                                                     MojoFailureException {
    parentMojo.handleException(exc);
  }

  protected abstract O executeInternal(I context) throws Exception;
}

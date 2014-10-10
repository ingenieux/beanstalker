package br.com.ingenieux.mojo.beanstalk.cmd.env.terminate;


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
public class TerminateEnvironmentContext {

  String environmentId;

  String environmentName;

  boolean terminateResources;

  /**
   * @return the environmentId
   */
  public String getEnvironmentId() {
    return environmentId;
  }

  /**
   * @param environmentId the environmentId to set
   */
  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  /**
   * @return the environmentName
   */
  public String getEnvironmentName() {
    return environmentName;
  }

  /**
   * @param environmentName the environmentName to set
   */
  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }

  /**
   * @return the terminateResources
   */
  public boolean isTerminateResources() {
    return terminateResources;
  }

  /**
   * @param terminateResources the terminateResources to set
   */
  public void setTerminateResources(boolean terminateResources) {
    this.terminateResources = terminateResources;
  }
}

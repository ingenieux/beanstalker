package br.com.ingenieux.mojo.beanstalk.cmd.env.swap;

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

public class SwapCNamesContext {

  /**
   * Source Environment Name
   */
  String sourceEnvironmentName;

  /**
   * Source Environment Id
   */
  String sourceEnvironmentId;

  /**
   * Destination Environment Name
   */
  String destinationEnvironmentName;

  /**
   * Destination Environment Id
   */
  String destinationEnvironmentId;

  /**
   * @return the sourceEnvironmentName
   */
  public String getSourceEnvironmentName() {
    return sourceEnvironmentName;
  }

  /**
   * @param sourceEnvironmentName the sourceEnvironmentName to set
   */
  public void setSourceEnvironmentName(String sourceEnvironmentName) {
    this.sourceEnvironmentName = sourceEnvironmentName;
  }

  /**
   * @return the sourceEnvironmentId
   */
  public String getSourceEnvironmentId() {
    return sourceEnvironmentId;
  }

  /**
   * @param sourceEnvironmentId the sourceEnvironmentId to set
   */
  public void setSourceEnvironmentId(String sourceEnvironmentId) {
    this.sourceEnvironmentId = sourceEnvironmentId;
  }

  /**
   * @return the destinationEnvironmentName
   */
  public String getDestinationEnvironmentName() {
    return destinationEnvironmentName;
  }

  /**
   * @param destinationEnvironmentName the destinationEnvironmentName to set
   */
  public void setDestinationEnvironmentName(String destinationEnvironmentName) {
    this.destinationEnvironmentName = destinationEnvironmentName;
  }

  /**
   * @return the destinationEnvironmentId
   */
  public String getDestinationEnvironmentId() {
    return destinationEnvironmentId;
  }

  /**
   * @param destinationEnvironmentId the destinationEnvironmentId to set
   */
  public void setDestinationEnvironmentId(String destinationEnvironmentId) {
    this.destinationEnvironmentId = destinationEnvironmentId;
  }
}

package br.com.ingenieux.mojo.beanstalk.cmd.env.create;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

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
public class CreateEnvironmentContext {

  String applicationName;

  String cnamePrefix;

  String applicationDescription;

  ConfigurationOptionSetting[] optionSettings = new ConfigurationOptionSetting[0];

  String environmentName;

  String versionLabel;

  String solutionStack;

  String templateName;
  String environmentTierName = "WebServer";
  String environmentTierType = "Standard";
  String environmentTierVersion = "1.0";

  /**
   * @return the applicationName
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * @param applicationName the applicationName to set
   */
  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * @return the cnamePrefix
   */
  public String getCnamePrefix() {
    return cnamePrefix;
  }

  /**
   * @param cnamePrefix the cnamePrefix to set
   */
  public void setCnamePrefix(String cnamePrefix) {
    this.cnamePrefix = cnamePrefix;
  }

  /**
   * @return the versionDescription
   */
  public String getApplicationDescription() {
    return applicationDescription;
  }

  /**
   * @param applicationDescription the applicationDescription to set
   */
  public void setApplicationDescription(String applicationDescription) {
    this.applicationDescription = applicationDescription;
  }

  /**
   * @return the optionSettings
   */
  public ConfigurationOptionSetting[] getOptionSettings() {
    return optionSettings;
  }

  /**
   * @param optionSettings the optionSettings to set
   */
  public void setOptionSettings(ConfigurationOptionSetting[] optionSettings) {
    if (null != optionSettings) {
      this.optionSettings = optionSettings;
    }
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
   * @return the versionLabel
   */
  public String getVersionLabel() {
    return versionLabel;
  }

  /**
   * @param versionLabel the versionLabel to set
   */
  public void setVersionLabel(String versionLabel) {
    this.versionLabel = versionLabel;
  }

  /**
   * @return the solutionStack
   */
  public String getSolutionStack() {
    return solutionStack;
  }

  /**
   * @param solutionStack the solutionStack to set
   */
  public void setSolutionStack(String solutionStack) {
    this.solutionStack = solutionStack;
  }

  /**
   * @return the templateName
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * @param templateName the templateName to set
   */
  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public String getEnvironmentTierName() {
    return environmentTierName;
  }

  public void setEnvironmentTierName(String environmentTierName) {
    this.environmentTierName = environmentTierName;
  }

  public String getEnvironmentTierType() {
    return environmentTierType;
  }

  public void setEnvironmentTierType(String environmentTierType) {
    this.environmentTierType = environmentTierType;
  }

  public String getEnvironmentTierVersion() {
    return environmentTierVersion;
  }

  public void setEnvironmentTierVersion(String environmentTierVersion) {
    this.environmentTierVersion = environmentTierVersion;
  }
}

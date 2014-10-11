package br.com.ingenieux.mojo.beanstalk;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

/**
 * Represents a Configuration Template
 *
 * @author Aldrin Leal
 */
public class ConfigurationTemplate {

  String id;
  ConfigurationOptionSetting[] optionSettings;
  String solutionStack;

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
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
    this.optionSettings = optionSettings;
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
}

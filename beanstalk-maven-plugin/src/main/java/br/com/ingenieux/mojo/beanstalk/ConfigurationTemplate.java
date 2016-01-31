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
 */

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

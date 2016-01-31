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

package br.com.ingenieux.mojo.beanstalk.util;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;

/**
 * Created by Aldrin on 16/10/2014.
 */
public class ConfigUtil {

  public static boolean optionSettingMatchesP(ConfigurationOptionSetting optionSetting, String namespace,
                                        String option) {
    return namespace.equals(optionSetting.getNamespace()) && option
        .equals(optionSetting.getOptionName());
  }
}

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

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

// CHECKSTYLE:OFF
/**
 * Source code generated by Fluent Builders Generator Do not modify this file See generator home
 * page at: http://code.google.com/p/fluent-builders-generator-eclipse-plugin/
 */
package br.com.ingenieux.mojo.beanstalk.cmd.env.update;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification;

public class UpdateEnvironmentContextBuilder extends UpdateEnvironmentContextBuilderBase<UpdateEnvironmentContextBuilder> {

  public UpdateEnvironmentContextBuilder() {
    super(new UpdateEnvironmentContext());
  }

  public static UpdateEnvironmentContextBuilder updateEnvironmentContext() {
    return new UpdateEnvironmentContextBuilder();
  }

  public UpdateEnvironmentContext build() {
    return getInstance();
  }
}

class UpdateEnvironmentContextBuilderBase<GeneratorT extends UpdateEnvironmentContextBuilderBase<GeneratorT>> {

  private UpdateEnvironmentContext instance;

  protected UpdateEnvironmentContextBuilderBase(UpdateEnvironmentContext aInstance) {
    instance = aInstance;
  }

  protected UpdateEnvironmentContext getInstance() {
    return instance;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentName(String aValue) {
    instance.setEnvironmentName(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentId(String aValue) {
    instance.setEnvironmentId(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withVersionLabel(String aValue) {
    instance.setVersionLabel(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentDescription(String aValue) {
    instance.setEnvironmentDescription(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withOptionSettings(ConfigurationOptionSetting[] aValue) {
    instance.setOptionSettings(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withOptionsToRemove(OptionSpecification[] aValue) {
    instance.setOptionsToRemove(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withTemplateName(String aValue) {
    instance.setTemplateName(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withLatestVersionLabel(String aValue) {
    instance.setLatestVersionLabel(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentTierName(String aValue) {
    instance.setEnvironmentTierName(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentTierType(String aValue) {
    instance.setEnvironmentTierType(aValue);

    return (GeneratorT) this;
  }

  @SuppressWarnings("unchecked")
  public GeneratorT withEnvironmentTierVersion(String aValue) {
    instance.setEnvironmentTierVersion(aValue);

    return (GeneratorT) this;
  }
}

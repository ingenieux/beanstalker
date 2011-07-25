package br.com.ingenieux.mojo.beanstalk;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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

import org.apache.commons.lang.builder.CompareToBuilder;

import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification;

public class OptionToRemove extends OptionSpecification implements
    Comparable<OptionSpecification> {
	@Override
	public int compareTo(OptionSpecification o) {
		return new CompareToBuilder().append(this.getNamespace(), o.getNamespace())
		    .append(this.getOptionName(), o.getOptionName()).toComparison();
	}
}

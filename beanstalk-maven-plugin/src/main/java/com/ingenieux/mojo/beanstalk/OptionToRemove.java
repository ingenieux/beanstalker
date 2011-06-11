package com.ingenieux.mojo.beanstalk;

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

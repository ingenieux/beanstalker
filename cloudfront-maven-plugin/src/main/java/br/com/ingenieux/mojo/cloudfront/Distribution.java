package br.com.ingenieux.mojo.cloudfront;

import static org.apache.commons.lang.StringUtils.isNotBlank;


public class Distribution {
	String id;

	String domainName;

	String s3Bucket;

	String includes;

	String excludes = "**/.svn/**";

	public boolean isCustomDistribution() {
		return !isS3Distribution();
	}

	public boolean isS3Distribution() {
		return isNotBlank(s3Bucket);
	}
}

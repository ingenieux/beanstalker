package br.com.ingenieux.mojo.beanstalk.env;

/**
 * Reference to an elastic beanstalk environment by it's CNAME attribute including only the left most subdomain.
 */
public class CNamePrefixEnvironmentReference extends AbstractEnvironmentReference {

    public CNamePrefixEnvironmentReference(String value) {
        super(value);
    }
}

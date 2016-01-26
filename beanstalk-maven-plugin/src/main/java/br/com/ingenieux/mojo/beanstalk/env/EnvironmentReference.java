package br.com.ingenieux.mojo.beanstalk.env;

/**
 * Reference to an elastic beanstalk environment.
 */
public interface EnvironmentReference {

    /**
     *
     * @return A string representing the environment reference literal value.
     */
    String getReferenceValue();
}

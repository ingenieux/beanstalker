package br.com.ingenieux.mojo.beanstalk.env;

/**
 * Simple implementation to store the raw value.
 */
public class AbstractEnvironmentReference implements EnvironmentReference {

    private String value;

    /**
     * Stores the raw reference value.
     *
     * @param value the raw reference value
     */
    public AbstractEnvironmentReference(String value) {
        this.value = value;
    }

    /**
     *
     * @return the raw environment reference value.
     */
    @Override
    public String getReferenceValue() {
      return value;
    }
}

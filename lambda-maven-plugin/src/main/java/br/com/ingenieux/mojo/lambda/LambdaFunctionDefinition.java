package br.com.ingenieux.mojo.lambda;

import org.apache.commons.lang.builder.CompareToBuilder;

import java.io.Serializable;

public class LambdaFunctionDefinition implements Serializable, Comparable<LambdaFunctionDefinition> {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    int memorySize;

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    int timeout;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    String handler;

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    @Override
    public int compareTo(LambdaFunctionDefinition o) {
        if (null == o)
            return -1;

        if (this == o)
            return 0;

        return new CompareToBuilder().append(this.name, o.name).toComparison();
    }
}
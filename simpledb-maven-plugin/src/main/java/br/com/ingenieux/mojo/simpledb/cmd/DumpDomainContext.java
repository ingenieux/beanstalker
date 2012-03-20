package br.com.ingenieux.mojo.simpledb.cmd;

import java.io.File;

public class DumpDomainContext {
    final File outputFile;

    final String domain;

    public DumpDomainContext(File outputFile, String domain) {
        this.outputFile = outputFile;
        this.domain = domain;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public String getDomain() {
        return domain;
    }
}

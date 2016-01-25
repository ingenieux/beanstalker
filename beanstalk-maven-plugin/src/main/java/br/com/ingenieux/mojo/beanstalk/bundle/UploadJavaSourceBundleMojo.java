package br.com.ingenieux.mojo.beanstalk.bundle;

import static br.com.ingenieux.mojo.beanstalk.util.SourceBundleUtil.PROCFILE;
import static br.com.ingenieux.mojo.beanstalk.util.SourceBundleUtil.assemble;
import static br.com.ingenieux.mojo.beanstalk.util.SourceBundleUtil.createSourceBundle;
import static br.com.ingenieux.mojo.beanstalk.util.SourceBundleUtil.validateProcfile;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Upload a packed zip file from java SE project to Amazon S3 for further deployment.
 *
 */
@Mojo(name="upload-java-source-bundle")
public class UploadJavaSourceBundleMojo extends AbstractUploadSourceBundleMojo {
    /**
     * Project base directory
     */
    @Parameter(defaultValue = "${basedir}")
    String basedir;

    /**
     * Zip file to be uploaded to S3 when the project is Java SE
     */
    @Parameter(property = "beanstalk.sourceBundleFile",
            defaultValue = "${project.build.directory}/${project.build.finalName}.${project.packaging}.zip")
    File sourceBundleFile;

    @Parameter(defaultValue = "${project.build.directory}/beanstalker")
    File beanstalkerFolder;

    protected File getSourceBundle() throws IOException {

        getLog().info(
                "Creating source bundle for Java SE application to "
                        + sourceBundleFile.getPath());

        validateProcfile(PROCFILE);
        assemble(artifactFile, beanstalkerFolder);
        createSourceBundle(beanstalkerFolder, sourceBundleFile);

        getLog().info("Source bundle file created");

        return sourceBundleFile;
    }
}

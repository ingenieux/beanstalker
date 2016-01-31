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

package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;

import java.io.File;
import java.util.Date;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import static java.lang.String.format;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 *
 * @since 0.2.8
 */
@Mojo(name = "fast-deploy")
public class FastDeployMojo extends AbstractNeedsEnvironmentMojo {

    /**
     * Artifact to Deploy
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    File sourceDirectory;

    /**
     * Git Staging Dir (should not be under target/)
     */
    @Parameter(property = "beanstalk.stagingDirectory",
            defaultValue = "${project.basedir}/tmp-git-deployment-staging")
    File stagingDirectory;

    /**
     * Use Staging Directory?
     */
    @Parameter(property = "beanstalk.useStagingDirectory", defaultValue = "false")
    boolean useStagingDirectory = false;

    @Parameter(defaultValue = "${project}")
    MavenProject project;

    /**
     * Version Description
     */
    @Parameter(property = "beanstalk.versionDescription", defaultValue = "Update from fast-deploy")
    String versionDescription;

    /**
     * Skip Environment Update?
     */
    @Parameter(property = "beanstalk.skipEnvironmentUpdate", defaultValue = "false")
    boolean skipEnvironmentUpdate = false;

    /**
     * Silent Upload?
     */
    @Parameter(property = "beanstalk.silentUpload", defaultValue = "false")
    boolean silentUpload = false;

    @Override
    protected void configure() {
        try {
            super.configure();
        } catch (Exception exc) {
            //getLog().warn(exc);
        }
    }

    @Override
    protected Object executeInternal() throws Exception {
        Git git = getGitRepo();
        String versionLabel = null;

        String commitId = null;

        Ref masterRef = git.getRepository()
                .getRef("master");
        if (null != masterRef) {
            commitId = ObjectId.toString(masterRef.getObjectId());
        }

        Status status = git.status().call();

        boolean pushAhead = false;

        if (null != commitId && status.isClean()) {
            versionLabel = lookupVersionLabelForCommitId(commitId);

            if (null == versionLabel) {
                getLog().info(
                        "No Changes. However, we've didn't get something close in AWS Elastic Beanstalk and we're pushing ahead");
                pushAhead = true;
            } else {
                getLog().info(
                        "No Changes. However, we've got something close in AWS Elastic Beanstalk and we're continuing");

                project.getProperties().put("beanstalk.versionLabel", versionLabel);

                return null;
            }
        }

        if (!pushAhead) {
            // Asks for Existing Files to get added
            git.add().setUpdate(true).addFilepattern(".").call();

            // Now as for any new files (untracked)

            AddCommand addCommand = git.add();

            if (!status.getUntracked().isEmpty()) {
                for (String s : status.getUntracked()) {
                    getLog().info("Adding file " + s);
                    addCommand.addFilepattern(s);
                }

                addCommand.call();
            }

            git.commit().setAll(true).setMessage(versionDescription).call();

            masterRef = git.getRepository()
                    .getRef("master");

            commitId = ObjectId.toString(masterRef.getObjectId());
        }

        String environmentName = null;

		/*
                 * Builds the remote push URL
		 */
        if (null != curEnv && !skipEnvironmentUpdate) {
            environmentName = curEnv.getEnvironmentName();
        }

        String remote = getRemoteUrl(commitId, environmentName);

        getLog().info("Using remote: " + remote);

		/*
                 * Does the Push
		 */
        {
            PushCommand cmd = git.//
                    push();

            if (!silentUpload) {
                cmd.setProgressMonitor(new TextProgressMonitor());
            }

            Iterable<PushResult> pushResults = null;
            try {
                pushResults = cmd.setRefSpecs(new RefSpec("HEAD:refs/heads/master")).//
                        setForce(true).//
                        setRemote(remote).//
                        call();
            } catch (Exception exc) {
                // Ignore
                getLog().debug("(Actually Expected) Exception", exc);
            }

			/*
                         * I wish someday it could work... :(
			 */
            if (null != pushResults) {
                for (PushResult pushResult : pushResults) {
                    getLog().debug(" * " + pushResult.getMessages());
                }
            }
        }

        versionLabel = lookupVersionLabelForCommitId(commitId);

        if (null != versionLabel) {
            getLog().info("Deployed version " + versionLabel);

            project.getProperties().put("beanstalk.versionLabel", versionLabel);
        } else {
            getLog().warn("No version found. Ignoring.");
        }

        return null;
    }

    protected String getRemoteUrl(String commitId, String environmentName) throws org.apache.maven.plugin.MojoFailureException {
        return new RequestSigner(getAWSCredentials(), applicationName,
                regionName, commitId, environmentName, new Date())
                .getPushUrl();
    }

    protected String lookupVersionLabelForCommitId(String commitId) throws Exception {
        String versionLabel = null;
        String prefixToLookup = format("git-%s-", commitId);

        DescribeApplicationVersionsResult
                describeApplicationVersions =
                getService().describeApplicationVersions(
                        new DescribeApplicationVersionsRequest().withApplicationName(applicationName));

        for (ApplicationVersionDescription avd : describeApplicationVersions.getApplicationVersions()) {
            if (avd.getVersionLabel().startsWith(prefixToLookup)) {
                versionLabel = avd.getVersionLabel();
                break;
            }
        }

        return versionLabel;
    }

    protected Git getGitRepo() throws Exception {
        Git git = null;

        if (!useStagingDirectory) {
            File gitRepo = new File(sourceDirectory, ".git");

            if (!gitRepo.exists()) {
                git = Git.init().setDirectory(sourceDirectory).call();
            } else {
                git = Git.open(gitRepo);
            }
        } else {
            File gitRepo = stagingDirectory;
            Repository r = null;

            RepositoryBuilder
                    b =
                    new RepositoryBuilder().setGitDir(stagingDirectory).setWorkTree(sourceDirectory);

            if (!gitRepo.exists()) {
                gitRepo.getParentFile().mkdirs();

                r = b.build();

                r.create();
            } else {
                r = b.build();
            }

            git = Git.wrap(r);
        }

        return git;
    }
}

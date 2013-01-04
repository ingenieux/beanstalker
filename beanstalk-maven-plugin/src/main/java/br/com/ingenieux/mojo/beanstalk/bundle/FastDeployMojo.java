package br.com.ingenieux.mojo.beanstalk.bundle;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

import java.io.File;
import java.util.Date;
import java.util.ListIterator;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 */
@MojoGoal("fast-deploy")
@MojoSince("0.2.8")
public class FastDeployMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * Artifact to Deploy
	 */
	@MojoParameter(expression = "${project.build.directory}/${project.build.finalName}")
	File sourceDirectory;

	@MojoParameter(expression = "${beanstalk.stagingDirectory}", description = "Git Staging Dir (should not be under target/)", defaultValue = "${project.basedir}/tmp-git-deployment-staging")
	File stagingDirectory;

	@MojoParameter(expression = "${beanstalk.useStagingDirectory}", description = "Use Staging Directory?", defaultValue = "false")
	boolean useStagingDirectory = false;
	
	/**
	 * Version Description
	 */
	@MojoParameter(expression="${beanstalk.versionDescription}", defaultValue="Update from fast-deploy")
	String versionDescription;

	@Override
	protected void configure() {
		try {
			super.configure();
		} catch (Exception exc) {
		}

		region = defaultIfBlank(region, "us-east-1");
	}

	@Override
	protected Object executeInternal() throws Exception {
		Git git = getGitRepo();

		Status status = git.status().call();

		if (status.isClean()) {
			getLog().info("No Changes");

			return null;
		}

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

		String commitId = ObjectId.toString(git.getRepository()
				.getRef("master").getObjectId());

		String environmentName = null;

		if (null != curEnv)
			environmentName = curEnv.getEnvironmentName();

		String remote = new RequestSigner(getAWSCredentials(), applicationName,
				region, commitId, environmentName, new Date()).getPushUrl();

		PushCommand cmd = git.//
				push();

		cmd.setProgressMonitor(new TextProgressMonitor());

		try {
			cmd.setRefSpecs(new RefSpec("HEAD:refs/heads/master")).//
					setForce(true).//
					setRemote(remote).//
					call();
		} catch (Exception exc) {
			// Ignore
		}

		String applicationVersionId = "Unknown";

		{
			String gitVersionBase = "git-" + commitId;
			DescribeApplicationVersionsResult describeApplicationVersions = getService()
					.describeApplicationVersions(
							new DescribeApplicationVersionsRequest()
									.withApplicationName(applicationName));
			boolean found = false;

			ListIterator<ApplicationVersionDescription> versions = describeApplicationVersions
					.getApplicationVersions().listIterator();
			
			while (! found) {
				ApplicationVersionDescription curVer = versions.next();
				
				found = curVer.getVersionLabel().startsWith(gitVersionBase);
				
				if (found) {
					applicationVersionId = curVer.getVersionLabel();
				}
			}
		}

		getLog().info("Deployed version " + applicationVersionId);

		return null;
	}

	private Git getGitRepo() throws Exception {
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
			
			RepositoryBuilder b = new RepositoryBuilder().setGitDir(stagingDirectory).setWorkTree(sourceDirectory);

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

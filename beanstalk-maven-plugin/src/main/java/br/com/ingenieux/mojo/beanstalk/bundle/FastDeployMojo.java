package br.com.ingenieux.mojo.beanstalk.bundle;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoSince;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

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
		File gitRepo = new File(sourceDirectory, ".git");

		if (gitRepo.exists())
			FileUtils.cleanDirectory(gitRepo);

		Git git = Git.init().setDirectory(sourceDirectory).call();

		git.add().addFilepattern(".").call();

		git.commit().setMessage("Update from fast-deploy").call();

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

		String gitVersion = "git-" + commitId;

		getLog().info("Deployed version " + gitVersion);

		return null;
	}
}

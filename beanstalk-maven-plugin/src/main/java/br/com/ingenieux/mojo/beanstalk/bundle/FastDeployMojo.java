package br.com.ingenieux.mojo.beanstalk.bundle;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.ObjectId;
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
		region = defaultIfBlank(region, "us-east-1");

		super.configure();
	}

	@Override
	protected Object executeInternal() throws Exception {
		File gitRepo = new File(sourceDirectory, ".git");

		Git git = null;

		if (gitRepo.exists()) {
			git = Git.open(sourceDirectory);
		} else {
			git = Git.init().setDirectory(sourceDirectory).call();
		}

		DirCache dirCache = git.add().addFilepattern(".").call();

		if (dirCache.isOutdated())
			git.commit().setMessage("Update from fast-deploy").call();

		String commitId = ObjectId.toString(git.getRepository()
				.getRef("master").getObjectId());

		String remote = new RequestSigner(getAWSCredentials(), applicationName,
				region, commitId, curEnv.getEnvironmentName(), new Date())
				.getPushUrl();

		git.push().setRefSpecs(new RefSpec("HEAD:refs/heads/master"))
				.setRemote(remote).call();

		return null;
	}
}

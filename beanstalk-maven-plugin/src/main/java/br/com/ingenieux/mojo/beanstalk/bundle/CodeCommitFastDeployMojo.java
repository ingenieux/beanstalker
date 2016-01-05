package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;

import java.io.File;
import java.util.Date;
import java.util.List;

import static br.com.ingenieux.mojo.aws.util.CredentialsUtil.redact;
import static br.com.ingenieux.mojo.aws.util.CredentialsUtil.redactRegardlessOfGit;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 *
 * @since 0.2.8
 */
@Mojo(name = "codecommit-fast-deploy")
public class CodeCommitFastDeployMojo extends FastDeployMojo {
    @Parameter(property = "beanstalk.codeCommitRepoName")
    String repoName;

    @Override
    protected void configure() {
        super.configure();
    }

    @Override
    protected String getRemoteUrl(String commitId, String environmentName) throws MojoFailureException {
        return new CodeCommitRequestSigner(getAWSCredentials(), repoName, new Date())
                .getPushUrl();
    }

    @Override
    protected Git getGitRepo() throws Exception {
        if (stagingDirectory.exists() && new File(stagingDirectory, "HEAD").exists()) {
            Git git = Git.open(stagingDirectory);

            git.fetch().
                    setRemote(getRemoteUrl(null, null)).
                    setProgressMonitor(new TextProgressMonitor()).
                    setRefSpecs(new RefSpec("refs/heads/master")).
                    call();
        } else {
            Git.cloneRepository().
                    setURI(getRemoteUrl(null, null)).
                    setProgressMonitor(new TextProgressMonitor()).
                    setDirectory(stagingDirectory).
                    setNoCheckout(true).
                    setBare(true).
                    call();
        }

        Repository r = null;

        RepositoryBuilder
                b =
                new RepositoryBuilder().
                        setGitDir(stagingDirectory).
                        setWorkTree(sourceDirectory);

        r = b.build();

        final Git git = Git.wrap(r);

        return git;
    }

    @Override
    protected String lookupVersionLabelForCommitId(String commitId) throws Exception {
        String s3Bucket = getService().createStorageLocation().getS3Bucket();

        ObjectNode payload = objectMapper.createObjectNode();

        payload.put("applicationName", applicationName);

        payload.put("commitId", commitId);

        payload.put("repoName", repoName);

        payload.put("description", versionDescription);

        payload.put("accessKey", getAWSCredentials().getCredentials().getAWSAccessKeyId());

        payload.put("secretKey", getAWSCredentials().getCredentials().getAWSSecretKey());

        payload.put("region", getRegion().getName());

        payload.put("targetPath", format("s3://%s/apps/%s/versions/git-%s.zip", s3Bucket, applicationName, commitId));

        AWSLambda lambda = getClientFactory().getService(AWSLambdaClient.class);

        final String payloadAsString = objectMapper.writeValueAsString(payload);

        getLog().info("Calling beanstalk-codecommit-deployer with arguments set to: " + redactRegardlessOfGit(payloadAsString));

        final InvokeResult invoke = lambda.invoke(new InvokeRequest().withFunctionName("beanstalk-codecommit-deployer").withPayload(payloadAsString));

        String resultAsString = new String(invoke.getPayload().array(), "utf-8");

        if (isNotBlank(invoke.getFunctionError())) {
            final String errorMessage = "Unexpected: " + invoke.getFunctionError();

            getLog().info(errorMessage);

            throw new RuntimeException(errorMessage);
        } else {
            List<String> messages = objectMapper.readValue(resultAsString, new TypeReference<List<String>>(){});

            for (String m : messages) {
                getLog().info(m);
            }
        }

        return super.lookupVersionLabelForCommitId(commitId);
    }
}

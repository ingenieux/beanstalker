package br.com.ingenieux.mojo.cloudformation;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import br.com.ingenieux.mojo.aws.util.BeanstalkerS3Client;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Pushes (creates/updates) a given stack
 */
@Mojo(name="push-stack")
public class PushStackMojo extends AbstractCloudformationMojo {
    @Parameter(property="cloudfront.stackLocation",
            required=true,
            defaultValue="${project.basedir}/src/main/cloudfront/${project.artifactId}.template.json")
    File templateLocation;

    /**
     * If set to true, ignores/skips in case of a missing active stack found
     */
    @Parameter(property="cloudformation.failIfMissing", defaultValue = "false")
    Boolean failIfMissing;

    /**
     * <p>
     * S3 URL &quot;s3://&lt;bucketName&gt;/&lt;keyPath&gt;&quot; of the S3 Location
     * </p>
     *
     * <p>If set, will upload the @{templateLocation} contents prior to issuing a stack create/update process</p>
     */
    @Parameter(property="cloudfront.s3Url")
    String s3Url;

    AmazonS3URI destinationS3Uri;

    /**
     * Template Input Parameters
     */
    @Parameter
    List<com.amazonaws.services.cloudformation.model.Parameter> parameters = new ArrayList<>();

    /**
     * Notification ARNs
     */
    @Parameter
    List<String> notificationArns;

    /**
     * <p>On Failure</p>
     *
     * <p>Either &quot;DO_NOTHING&quot;, &quot;ROLLBACK&quot; or &quot;DELETE&quot;</p>
     */
    @Parameter(property = "cloudfront.onfailure", defaultValue="DO_NOTHING")
    OnFailure onFailure = OnFailure.DO_NOTHING;

    /**
     * Resource Types
     */
    @Parameter
    Collection<String> resourceTypes = new ArrayList<>();

    /**
     * Disable Rollback?
     */
    @Parameter(defaultValue = "true")
    Boolean disableRollback = true;

    /**
     * Tags
     */
    @Parameter
    List<Tag> tags = new ArrayList<>();

    /**
     * Timeout in Minutes
     */
    @Parameter(property = "cloudfront.timeoutInMinutes")
    Integer timeoutInMinutes;

    /**
     * Use Previous Template?
     */
    @Parameter(property = "usePreviousTemplate", defaultValue="true")
    Boolean usePreviousTemplate = true;

    BeanstalkerS3Client s3Client;

    @Override
    protected Object executeInternal() throws Exception {
        if (shouldFailIfMissingStack(failIfMissing)) {
            return null;
        };

        if (isNotBlank(s3Url)) {
            getLog().info("Uploading file " +
                    this.templateLocation +
                    " to location " +
                    this.s3Url);

            s3Client = new BeanstalkerS3Client(
                    getAWSCredentials(),
                    getClientConfiguration(),
                    getRegion());

            s3Client.setMultipartUpload(false);

            this.destinationS3Uri = new AmazonS3URI(s3Url);

            uploadContents(templateLocation, destinationS3Uri);
        }

        Object result = null;

        if (null == stackSummary) {
            getLog().info("Must Create Stack");

            result = createStack();
        } else {
            getLog().info("Must Update Stack");

            result = updateStack();
        }

        return result;
    }

    private CreateStackResult createStack() throws Exception {
        CreateStackRequest req = new CreateStackRequest()
                .withStackName(stackName)
                .withCapabilities(Capability.CAPABILITY_IAM);

        if (null == this.destinationS3Uri) {
            req.withTemplateURL(generateExternalUrl(this.destinationS3Uri));
        } else {
            req.withTemplateBody(IOUtils.toString(new FileInputStream(this.templateLocation)));
        }

        req.withNotificationARNs(notificationArns);

        req.withParameters(parameters);
        req.withResourceTypes(resourceTypes);
        req.withDisableRollback(disableRollback);
        req.withTags(tags);
        req.withTimeoutInMinutes(timeoutInMinutes);

        return getService().createStack(req);
    }

    protected String generateExternalUrl(AmazonS3URI destinationS3Uri) throws Exception {
        return s3Client.getResourceUrl(destinationS3Uri.getBucket(), destinationS3Uri.getKey());
    }

    private UpdateStackResult updateStack() throws Exception {
        UpdateStackRequest req = new UpdateStackRequest()
                .withStackName(stackName)
                .withCapabilities(Capability.CAPABILITY_IAM);

        if (null == this.destinationS3Uri) {
            req.withTemplateURL(generateExternalUrl(this.destinationS3Uri));
        } else {
            req.withTemplateBody(IOUtils.toString(new FileInputStream(this.templateLocation)));
        }

        req.withNotificationARNs(notificationArns);

        req.withParameters(parameters);
        req.withResourceTypes(resourceTypes);
        req.withUsePreviousTemplate(usePreviousTemplate);
        req.withTags(tags);

        return getService().updateStack(req);
    }

    private void uploadContents(File templateLocation, AmazonS3URI destinationS3Uri) throws Exception {
        s3Client.putObject(destinationS3Uri.getBucket(), destinationS3Uri.getKey(), new FileInputStream(this.templateLocation), null);
    }
}

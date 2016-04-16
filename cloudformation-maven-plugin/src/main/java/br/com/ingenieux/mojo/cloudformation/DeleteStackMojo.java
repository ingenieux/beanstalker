package br.com.ingenieux.mojo.cloudformation;

import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.StackStatus;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.ingenieux.mojo.cloudformation.cmd.WaitForStackCommand;

import static java.util.Arrays.asList;

/**
 * Waits until timeout and/or stack status, printing messages along the way
 */
@Mojo(name="delete-stack")
public class DeleteStackMojo extends AbstractCloudformationMojo {
    /**
     * If set to true, ignores/skips in case of a missing active stack found
     */
    @Parameter(property="cloudformation.failIfMissing", defaultValue = "false")
    Boolean failIfMissing;

    @Parameter(property="cloudformation.retainResources")
    List<String> retainResources = new ArrayList<>();

    public void setRetainResources(String resources) {
        retainResources.addAll(asList(resources.split("\\,")));
    }

    @Override
    protected Object executeInternal() throws Exception {
        shouldFailIfMissingStack(failIfMissing);

        getService().deleteStack(new DeleteStackRequest().withStackName(this.stackName).withRetainResources(retainResources));

        WaitForStackCommand.WaitForStackContext ctx = new WaitForStackCommand.WaitForStackContext(
                this.stackName,
                getService(),
                this,
                30,
                asList(StackStatus.DELETE_COMPLETE)
        );

        new WaitForStackCommand(ctx).execute();

        return null;
    }
}

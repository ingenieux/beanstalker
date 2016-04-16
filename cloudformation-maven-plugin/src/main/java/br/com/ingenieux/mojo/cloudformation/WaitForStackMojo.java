package br.com.ingenieux.mojo.cloudformation;

import com.amazonaws.services.cloudformation.model.StackStatus;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.ingenieux.mojo.cloudformation.cmd.WaitForStackCommand;

/**
 * Waits until timeout and/or stack status, printing messages along the way
 */
@Mojo(name="wait-for-stack")
public class WaitForStackMojo extends AbstractCloudformationMojo {
    /**
     * If set to true, ignores/skips in case of a missing active stack found
     */
    @Parameter(property="cloudformation.failIfMissing", defaultValue = "false")
    Boolean failIfMissing;

    @Parameter(property="cloudformation.timeoutMins", defaultValue="15")
    Integer timeoutMins;

    @Parameter(property="cloudformation.statusesToMatch")
    Set<StackStatus> statusesToMatch = new LinkedHashSet<>();

    public void setStatusesToMatch(String statusesToMatch) {
        this.statusesToMatch = new LinkedHashSet<>();

        this.statusesToMatch.addAll(
                Arrays.asList(statusesToMatch.split(",")).stream()
                    .map(this::extractNVPair)
                    .map(x -> StackStatus.fromValue(x.getValue().toUpperCase()))
                    .collect(Collectors.toList())
        );
    }

    @Override
    protected Object executeInternal() throws Exception {
        shouldFailIfMissingStack(failIfMissing);

        if (null == stackSummary) {
            getLog().warn("No Stack Summary found. Skipping.");

            return null;
        }

        WaitForStackCommand.WaitForStackContext ctx = new WaitForStackCommand.WaitForStackContext(
                this.stackSummary.getStackName(),
                getService(),
                this,
                timeoutMins,
                statusesToMatch);

        new WaitForStackCommand(ctx).execute();

        return stackSummary;
    }
}

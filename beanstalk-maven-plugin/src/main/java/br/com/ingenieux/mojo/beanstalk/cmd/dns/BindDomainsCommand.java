package br.com.ingenieux.mojo.beanstalk.cmd.dns;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;
import br.com.ingenieux.mojo.beanstalk.cmd.BaseCommand;
import br.com.ingenieux.mojo.beanstalk.util.ConfigUtil;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.strip;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class BindDomainsCommand extends
                                BaseCommand<BindDomainsContext, Void> {

  private final AmazonRoute53 r53;
  private final AmazonEC2 ec2;
  private final AmazonElasticLoadBalancing elb;

  /**
   * Constructor
   *
   * @param parentMojo parent mojo
   */
  public BindDomainsCommand(AbstractNeedsEnvironmentMojo parentMojo)
      throws AbstractMojoExecutionException {
    super(parentMojo);

    try {
      this.r53 = parentMojo.getClientFactory().getService(AmazonRoute53Client.class);
      this.ec2 = parentMojo.getClientFactory().getService(AmazonEC2Client.class);
      this.elb = parentMojo.getClientFactory().getService(AmazonElasticLoadBalancingClient.class);

    } catch (Exception exc) {
      throw new MojoExecutionException("Failure", exc);
    }
  }

  protected boolean isSingleInstance(EnvironmentDescription env) {
    Validate.isTrue("WebServer".equals(env.getTier().getName()), "Not a Web Server environment!");

    final DescribeConfigurationSettingsResult
        describeConfigurationSettingsResult =
        parentMojo.getService().describeConfigurationSettings(
            new DescribeConfigurationSettingsRequest()
                .withApplicationName(env.getApplicationName())
                .withEnvironmentName(env.getEnvironmentName()));

    Validate.isTrue(1 == describeConfigurationSettingsResult.getConfigurationSettings().size(),
                    "There should be one environment");

    final List<ConfigurationOptionSetting>
        optionSettings =
        describeConfigurationSettingsResult.getConfigurationSettings().get(0).getOptionSettings();

    for (ConfigurationOptionSetting optionSetting : optionSettings) {
      if (ConfigUtil.optionSettingMatchesP(optionSetting, "aws:elasticbeanstalk:environment",
                                           "EnvironmentType")) {
        return "SingleInstance".equals(optionSetting.getValue());
      }
    }

    throw new IllegalStateException("Unreachable code!");
  }

  @Override
  protected Void executeInternal(BindDomainsContext ctx) throws Exception {
    Map<String, String> recordsToAssign = new LinkedHashMap<String, String>();

    ctx.singleInstance = isSingleInstance(ctx.getCurEnv());

    /**
     * Step #2: Validate Parameters
     */
    {
      for (String domain : ctx.getDomains()) {
        String key = formatDomain(domain);
        String value = null;

				/*
                                 * Handle Entries in the form <record>:<zoneid>
				 */
        if (-1 != key.indexOf(':')) {
          String[] pair = key.split(":", 2);

          key = formatDomain(pair[0]);
          value = strip(pair[1], ".");
        }

        recordsToAssign.put(key, value);
      }

      Validate.isTrue(recordsToAssign.size() > 0, "No Domains Supplied!");

      if (isInfoEnabled()) {
        info("Domains to Map to Environment (cnamePrefix='%s')", ctx.getCurEnv().getCNAME());

        for (Map.Entry<String, String> entry : recordsToAssign.entrySet()) {
          String key = entry.getKey();
          String zoneId = entry.getValue();

          String message = format(" * Domain: %s", key);

          if (null != zoneId) {
            message += " (and using zoneId " + zoneId + ")";
          }

          info(message);
        }
      }
    }

    /**
     * Step #3: Lookup Domains on Route53
     */
    Map<String, HostedZone> hostedZoneMapping = new LinkedHashMap<String, HostedZone>();

    {
      Set<String> unresolvedDomains = new LinkedHashSet<String>();

      for (Map.Entry<String, String> entry : recordsToAssign.entrySet()) {
        if (null != entry.getValue()) {
          continue;
        }

        unresolvedDomains.add(entry.getKey());
      }

      for (HostedZone hostedZone : r53.listHostedZones().getHostedZones()) {
        String id = hostedZone.getId();
        String name = hostedZone.getName();

        hostedZoneMapping.put(id, hostedZone);

        if (unresolvedDomains.contains(name)) {
          if (isInfoEnabled()) {
            info("Mapping Domain %s to R53 Zone Id %s", name, id);
          }

          recordsToAssign.put(name, id);

          unresolvedDomains.remove(name);
        }
      }

      Validate.isTrue(unresolvedDomains.isEmpty(),
                      "Domains not resolved: " + join(unresolvedDomains, "; "));
    }

    /**
     * Step #4: Domain Validation
     */
    {
      for (Map.Entry<String, String> entry : recordsToAssign.entrySet()) {
        String record = entry.getKey();
        String zoneId = entry.getValue();
        HostedZone hostedZone = hostedZoneMapping.get(zoneId);

        Validate.notNull(hostedZone,
                         format("Unknown Hosted Zone Id: %s for Record: %s", zoneId, record));
        Validate.isTrue(record.endsWith(hostedZone.getName()),
                        format("Record %s does not map to zoneId %s (domain: %s)", record, zoneId,
                               hostedZone.getName()));
      }
    }

    /**
     * Step #5: Get ELB Hosted Zone Id - if appliable
     */
    if (!ctx.singleInstance) {
      String
          loadBalancerName =
          parentMojo.getService().describeEnvironmentResources(
              new DescribeEnvironmentResourcesRequest()
                  .withEnvironmentId(ctx.getCurEnv().getEnvironmentId())).getEnvironmentResources()
              .getLoadBalancers().get(0).getName();

      DescribeLoadBalancersRequest req = new DescribeLoadBalancersRequest(
          asList(loadBalancerName));

      List<LoadBalancerDescription> loadBalancers = elb.describeLoadBalancers(req)
          .getLoadBalancerDescriptions();

      Validate.isTrue(1 == loadBalancers.size(), "Unexpected number of Load Balancers returned");

      ctx.elbHostedZoneId = loadBalancers.get(0).getCanonicalHostedZoneNameID();

      if (isInfoEnabled()) {
        info(format("Using ELB Canonical Hosted Zone Name Id %s", ctx.elbHostedZoneId));
      }
    }

    /**
     * Step #6: Apply Change Batch on Each Domain
     */
    for (Map.Entry<String, String> recordEntry : recordsToAssign.entrySet()) {
      assignDomain(ctx, recordEntry.getKey(), recordEntry.getValue());
    }

    return null;
  }

  protected void assignDomain(BindDomainsContext ctx, String record, String zoneId) {
    ChangeBatch changeBatch = new ChangeBatch();

    changeBatch.setComment(format("Updated for env %s", ctx.getCurEnv().getCNAME()));

    /**
     * Look for Existing Resource Record Sets
     */
    {
      ResourceRecordSet resourceRecordSet = null;

      ListResourceRecordSetsResult listResourceRecordSets = r53
          .listResourceRecordSets(new ListResourceRecordSetsRequest(
              zoneId));

      for (ResourceRecordSet rrs : listResourceRecordSets
          .getResourceRecordSets()) {
        if (!rrs.getName().equals(record)) {
          continue;
        }

        boolean matchesTypes = "A".equals(rrs.getType()) || "CNAME".equals(rrs.getType());

        if (!matchesTypes) {
          continue;
        }

        if (isInfoEnabled()) {
          info("Excluding resourceRecordSet %s for domain %s", rrs, record);
        }

        changeBatch.getChanges().add(new Change(ChangeAction.DELETE,
                                                rrs));
      }
    }

    /**
     * Then Add Ours
     */
    ResourceRecordSet resourceRecordSet = new ResourceRecordSet();

    resourceRecordSet.setName(record);
    resourceRecordSet.setType(RRType.A);

    if (ctx.singleInstance) {
      final String address = ctx.getCurEnv().getEndpointURL();
      ResourceRecord resourceRecord = new ResourceRecord(address);

      resourceRecordSet.setTTL(60L);
      resourceRecordSet.setResourceRecords(asList(resourceRecord));

      if (isInfoEnabled()) {
        info("Adding resourceRecordSet %s for domain %s mapped to %s", resourceRecordSet, record,
             address);
      }
    } else {
      AliasTarget aliasTarget = new AliasTarget();

      aliasTarget.setHostedZoneId(ctx.getElbHostedZoneId());
      aliasTarget.setDNSName(ctx.getCurEnv().getEndpointURL());

      resourceRecordSet.setAliasTarget(aliasTarget);

      if (isInfoEnabled()) {
        info("Adding resourceRecordSet %s for domain %s mapped to %s", resourceRecordSet, record,
             aliasTarget.getDNSName());
      }
    }

    changeBatch.getChanges().add(new Change(ChangeAction.CREATE, resourceRecordSet));

    if (isInfoEnabled()) {
      info("Changes to be sent: %s", changeBatch.getChanges());
    }

    ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest(
        zoneId, changeBatch);

    r53.changeResourceRecordSets(req);
  }

  String formatDomain(String d) {
    return strip(d, ".").concat(".");
  }
}

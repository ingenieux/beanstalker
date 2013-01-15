package br.com.ingenieux.mojo.beanstalk.dns;

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

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.strip;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
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
import com.amazonaws.services.route53.model.ResourceRecordSet;

/**
 * <p>
 * Binds an Elastic Beanstalk Environment into a set of Route53 records
 * </p>
 * 
 * <p>
 * NOTE: THIS IS HIGHLY EXPERIMENTAL CODE
 * </p>
 * 
 * @since 0.2.9
 */
@Mojo(name = "bind-domains")
public class BindDomainsMojo extends AbstractNeedsEnvironmentMojo {
	/**
	 * <p>List of Domains</p>
	 * 
	 * <p>Could be set as either:</p>
	 * <ul>
	 * <li>fqdn:hostedZoneId (e.g. "services.modafocas.org:Z3DJ4DL0DIEEJA")</li>
	 * <li>hosted zone name - will be set to root. (e.g., "modafocas.org")</li>
	 * </ul>
	 */
	@Parameter(property = "beanstalk.domains")
	String[] domains;

	@Override
	protected Object executeInternal() throws Exception {
		Map<String, String> recordsToAssign = new LinkedHashMap<String, String>();
		/**
		 * Step #1: AWS Client Config
		 */
		BindDomainContext ctx = new BindDomainContext();

		/**
		 * Step #2: Validate Parameters
		 */
		{
			for (String domain : domains) {
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

			if (getLog().isInfoEnabled()) {
				getLog().info(format("Domains to Map to Environment (cnamePrefix='%s')", curEnv.getCNAME()));

				for (Entry<String, String> entry : recordsToAssign.entrySet()) {
					String key = entry.getKey();
					String zoneId = entry.getValue();
					
					String message = format(" * Domain: %s", key);
					
					if (null != zoneId)
						message += " (and using zoneId " + zoneId + ")";
					
					getLog().info(message);
				}
			}
		}

		/**
		 * Step #3: Lookup Domains on Route53
		 */
		Map<String, HostedZone> hostedZoneMapping = new LinkedHashMap<String, HostedZone>();
		
		{
			Set<String> unresolvedDomains = new LinkedHashSet<String>();
			
			for (Entry<String, String> entry : recordsToAssign.entrySet()) {
				if (null != entry.getValue())
					continue;
				
				unresolvedDomains.add(entry.getKey());
			}
			
			for (HostedZone hostedZone : ctx.r53.listHostedZones().getHostedZones()) {
				String id = hostedZone.getId();
				String name = hostedZone.getName();
				
				hostedZoneMapping.put(id, hostedZone);

				if (unresolvedDomains.contains(name)) {
					if (getLog().isInfoEnabled())
						getLog().info(format("Mapping Domain %s to R53 Zone Id %s", name, id));

					recordsToAssign.put(name, id);

					unresolvedDomains.remove(name);
				}
			}

			Validate.isTrue(unresolvedDomains.isEmpty(), "Domains not resolved: " + join(unresolvedDomains, "; "));
		}
		
		/**
		 * Step #4: Domain Validation
		 */
		{
			for (Entry<String, String> entry : recordsToAssign.entrySet()) {
				String record = entry.getKey();
				String zoneId = entry.getValue();
				HostedZone hostedZone = hostedZoneMapping.get(zoneId);
				
				Validate.notNull(hostedZone, format("Unknown Hosted Zone Id: %s for Record: %s", zoneId, record));
				Validate.isTrue(record.endsWith(hostedZone.getName()), format("Record %s does not map to zoneId %s (domain: %s)", record, zoneId, hostedZone.getName()));
			}
		}

		/**
		 * Step #5: Get ELB Hosted Zone Id
		 */
		{
			String loadBalancerName = getService().describeEnvironmentResources(new DescribeEnvironmentResourcesRequest().withEnvironmentId(curEnv.getEnvironmentId())).getEnvironmentResources().getLoadBalancers().get(0).getName();

			DescribeLoadBalancersRequest req = new DescribeLoadBalancersRequest(
					Arrays.asList(loadBalancerName));

			List<LoadBalancerDescription> loadBalancers = ctx.elb.describeLoadBalancers(req)
					.getLoadBalancerDescriptions();

			Validate.isTrue(1 == loadBalancers.size(), "Unexpected number of Load Balancers returned");

			ctx.elbHostedZoneId = loadBalancers.get(0).getCanonicalHostedZoneNameID();

			if (getLog().isInfoEnabled())
				getLog().info(format("Using ELB Canonical Hosted Zone Name Id %s", ctx.elbHostedZoneId));
		}

		/**
		 * Step #6: Apply Change Batch on Each Domain
		 */
		for (Entry<String, String> recordEntry : recordsToAssign.entrySet()) {
			assignDomain(ctx, recordEntry.getKey(), recordEntry.getValue());
		}

		return null;
	}

	protected void assignDomain(BindDomainContext ctx, String record, String zoneId) {
		ChangeBatch changeBatch = new ChangeBatch();

		/**
		 * Look for Existing Resource Record Sets
		 */
		{
			ResourceRecordSet resourceRecordSet = null;

			ListResourceRecordSetsResult listResourceRecordSets = ctx.r53
					.listResourceRecordSets(new ListResourceRecordSetsRequest(
							zoneId));

			for (ResourceRecordSet rrs : listResourceRecordSets
					.getResourceRecordSets()) {
				if (!rrs.getName().equals(record))
					continue;

				if (!"A".equals(rrs.getType()))
					continue;

				resourceRecordSet = rrs;
				break;
			}

			if (null != resourceRecordSet) {
				if (getLog().isInfoEnabled())
					getLog().info(format("Excluding resourceRecordSet %s for domain %s", resourceRecordSet, record));
				changeBatch.getChanges().add(new Change(ChangeAction.DELETE,
						resourceRecordSet));
			}
		}

		/**
		 * Then Add Ours
		 */
		AliasTarget aliasTarget = new AliasTarget();

		aliasTarget.setHostedZoneId(ctx.elbHostedZoneId);
		aliasTarget.setDNSName(curEnv.getEndpointURL());

		ResourceRecordSet resourceRecordSet = new ResourceRecordSet();

		resourceRecordSet.setName(record);
		resourceRecordSet.setType(RRType.A);

		resourceRecordSet.setAliasTarget(aliasTarget);

		if (getLog().isInfoEnabled())
			getLog().info(format("Adding resourceRecordSet %s for domain %s mapped to %s", resourceRecordSet, record, aliasTarget.getDNSName()));

		changeBatch.getChanges().add(new Change(ChangeAction.CREATE, resourceRecordSet));

		ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest(
				zoneId, changeBatch);

		ctx.r53.changeResourceRecordSets(req);
	}

	class BindDomainContext {
		String elbHostedZoneId;

		AmazonRoute53 r53;

		AmazonEC2 ec2;

		AmazonElasticLoadBalancing elb;

		public BindDomainContext() throws ReflectiveOperationException {
			this.r53 = clientFactory.getService(AmazonRoute53Client.class);
			this.ec2 = clientFactory.getService(AmazonEC2Client.class);
			this.elb = clientFactory.getService(AmazonElasticLoadBalancingClient.class);
		}

		public void setElbHostedZoneId(String elbHostedZoneId) {
			this.elbHostedZoneId = elbHostedZoneId;
		}

		public String getElbHostedZoneId() {
			return elbHostedZoneId;
		}

	}

	String formatDomain(String d) {
		return strip(d, ".").concat(".");
	}
}

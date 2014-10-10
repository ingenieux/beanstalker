package br.com.ingenieux.mojo.beanstalk.env;

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

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesResult;
import com.amazonaws.services.elasticbeanstalk.model.Instance;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import br.com.ingenieux.mojo.beanstalk.AbstractNeedsEnvironmentMojo;

/**
 * Dump Environment Instance Addresses
 *
 * See the docs for <a href= "http://docs.amazonwebservices.com/elasticbeanstalk/latest/api/API_DescribeEnvironmentResources.html"
 * >DescribeEnvironmentResources API</a> call.
 *
 * @author Aldrin Leal
 * @since 1.1.1
 */
@Mojo(name = "dump-instances", requiresDirectInvocation = true)
public class DumpInstancesMojo extends AbstractNeedsEnvironmentMojo {

  /**
   * (Optional) output file to output to
   */
  @Parameter(property = "beanstalk.outputFile")
  private File outputFile;

  /**
   * Dump private addresses? defaults to false
   */
  @Parameter(property = "beanstalk.dumpPrivateAddresses", defaultValue = "false")
  private boolean dumpPrivateAddresses;

  @Override
  protected Object executeInternal() throws Exception {
    AmazonEC2 ec2 = clientFactory.getService(AmazonEC2Client.class);

    DescribeEnvironmentResourcesResult envResources = getService()
        .describeEnvironmentResources(
            new DescribeEnvironmentResourcesRequest()
                .withEnvironmentId(curEnv.getEnvironmentId())
                .withEnvironmentName(
                    curEnv.getEnvironmentName()));
    List<String> instanceIds = new ArrayList<String>();

    for (Instance i : envResources.getEnvironmentResources().getInstances()) {
      instanceIds.add(i.getId());
    }

    DescribeInstancesResult ec2Instances = ec2
        .describeInstances(new DescribeInstancesRequest()
                               .withInstanceIds(instanceIds));

    PrintStream printStream = null;

    if (null != outputFile) {
      printStream = new PrintStream(outputFile);
    }

    for (Reservation r : ec2Instances.getReservations()) {
      for (com.amazonaws.services.ec2.model.Instance i : r.getInstances()) {
        String ipAddress = dumpPrivateAddresses ? i
            .getPrivateIpAddress() : StringUtils.defaultString(
            i.getPublicIpAddress(), i.getPrivateDnsName());
        String instanceId = i.getInstanceId();

        if (null != printStream) {
          printStream.println(ipAddress + " # " + instanceId);
        } else {
          getLog().info(" * " + instanceId + ": " + ipAddress);
        }
      }
    }

    if (null != printStream) {
      printStream.close();
    }

    return null;
  }
}

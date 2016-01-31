/*
 * Copyright (c) 2016 ingenieux Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ingenieux.mojo.aws.util;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.commons.lang.reflect.ConstructorUtils;

import java.lang.reflect.InvocationTargetException;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class AWSClientFactory {

    private AWSCredentialsProvider creds;

    private ClientConfiguration clientConfiguration;

    private String region;

    public AWSClientFactory(AWSCredentialsProvider creds, ClientConfiguration clientConfiguration,
                            String region) {
        this.creds = creds;
        this.clientConfiguration = clientConfiguration;
        this.region = region;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClazz)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
            InstantiationException {
        T
                resultObj =
                (T) ConstructorUtils
                        .invokeConstructor(serviceClazz, new Object[]{creds, clientConfiguration},
                                new Class<?>[]{AWSCredentialsProvider.class,
                                        ClientConfiguration.class});

        if (isNotBlank(region)) {
            for (ServiceEndpointFormatter formatter : ServiceEndpointFormatter.values()) {
                if (formatter.matches(resultObj)) {
                    ((AmazonWebServiceClient) resultObj).setEndpoint(getEndpointFor(formatter));
                    break;
                }

                // extra fix for eu-central-1
                if (resultObj instanceof AmazonS3Client) {
                    ((AmazonS3Client) resultObj).setRegion(RegionUtils.getRegion(region));
                }
            }
        }

        return resultObj;
    }

    protected String getEndpointFor(ServiceEndpointFormatter formatter) {
        return format(formatter.serviceMask, region);
    }

}

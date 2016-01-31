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

package br.com.ingenieux.mojo.beanstalk;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;

import junit.framework.TestCase;

import br.com.ingenieux.mojo.aws.util.TypeUtil;
import br.com.ingenieux.mojo.beanstalk.bundle.CreateStorageLocationMojo;

public class TestTypeUtil extends TestCase {

  public void testTypeUtil() {
    Class<?> c = CreateStorageLocationMojo.class;

    Class<?> serviceClass = TypeUtil.getServiceClass(c);

    assertEquals(serviceClass.getName(), AWSElasticBeanstalkClient.class.getName());

  }

}

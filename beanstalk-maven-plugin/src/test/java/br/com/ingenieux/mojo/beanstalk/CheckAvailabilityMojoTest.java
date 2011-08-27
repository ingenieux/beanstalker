package br.com.ingenieux.mojo.beanstalk;

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
import junit.framework.Assert;

public class CheckAvailabilityMojoTest extends BeanstalkTestBase {
	public void testCheckAvailability() throws Exception {
		setVariableValueToObject(checkAvailabilityMojo, "cnamePrefix",
		    "bmp-demo-" + System.currentTimeMillis());

		checkAvailabilityMojo.execute();
	}

	public void testFailWhenExists() throws Exception {
		setVariableValueToObject(checkAvailabilityMojo, "failWhenExists", true);
		setVariableValueToObject(checkAvailabilityMojo, "cnamePrefix", "amazon");

		try {
			checkAvailabilityMojo.execute();

			Assert.fail("Didn't throw up exception");
		} catch (Exception e) {

		}
	}
}

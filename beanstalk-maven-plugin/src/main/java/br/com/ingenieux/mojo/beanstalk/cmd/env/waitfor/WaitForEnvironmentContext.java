package br.com.ingenieux.mojo.beanstalk.cmd.env.waitfor;

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
public class WaitForEnvironmentContext {
	String applicationName;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	Integer timeoutMins = 20;

	public Integer getTimeoutMins() {
		return timeoutMins;
	}

	public void setTimeoutMins(Integer timeoutMins) {
		this.timeoutMins = timeoutMins;
	}

	String statusToWaitFor;

	public String getStatusToWaitFor() {
		return statusToWaitFor;
	}

	public void setStatusToWaitFor(String statusToWaitFor) {
		this.statusToWaitFor = statusToWaitFor;
	}

	String environmentId;

	/**
	 * @return the environmentId
	 */
	public String getEnvironmentId() {
		return environmentId;
	}

	/**
	 * @param environmentId
	 *          the environmentId to set
	 */
	public void setEnvironmentId(String environmentId) {
		this.environmentId = environmentId;
	}

	String domainToWaitFor;

	/**
	 * @return the domainToWaitFor
	 */
	public String getDomainToWaitFor() {
		return domainToWaitFor;
	}

	/**
	 * @param domainToWaitFor
	 *          the domainToWaitFor to set
	 */
	public void setDomainToWaitFor(String domainToWaitFor) {
		this.domainToWaitFor = domainToWaitFor;
	}

    String health;

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    String workerEnvironmentName;

    public String getWorkerEnvironmentName() {
        return workerEnvironmentName;
    }

    public void setWorkerEnvironmentName(String workerEnvironmentName) {
        this.workerEnvironmentName = workerEnvironmentName;
    }
}

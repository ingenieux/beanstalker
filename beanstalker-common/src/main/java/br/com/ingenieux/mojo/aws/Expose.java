package br.com.ingenieux.mojo.aws;

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

/**
 * Represents a Server Security to Expose Locally (via Maven Project Properties)
 *
 * @author Aldrin Leal
 */
public class Expose {

  String serverId;

  String accessKey;

  String sharedKey;

  public String getServerId() {
    return serverId;
  }

  public void setServerId(String serverId) {
    this.serverId = serverId;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSharedKey() {
    return sharedKey;
  }

  public void setSharedKey(String sharedKey) {
    this.sharedKey = sharedKey;
  }
}

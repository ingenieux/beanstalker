package br.com.ingenieux.mojo.beanstalk.bundle;

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

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Uploads a packed war file to Amazon S3 for further Deployment.
 *
 * @since 0.1.0
 */
@Mojo(name = "upload-source-bundle")
public class UploadSourceBundleMojo extends AbstractUploadSourceBundleMojo {

  protected File getSourceBundle() {
      return artifactFile;
  }

}

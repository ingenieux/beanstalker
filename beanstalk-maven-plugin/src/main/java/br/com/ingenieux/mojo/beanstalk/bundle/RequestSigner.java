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

package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.auth.AWSCredentialsProvider;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class RequestSigner extends RequestSignerBase {
    String applicationId;

    String commitId;

    String environmentName;

    Date date;

    public RequestSigner(AWSCredentialsProvider awsCredentials, String applicationId,
                         String region, String commitId, String environmentName, Date date) {
        super(awsCredentials.getCredentials(), region, "devtools", date);
        this.applicationId = applicationId;
        this.commitId = commitId;
        this.environmentName = environmentName;
    }

    public String getPushUrl() {
        String user = awsCredentials.getAWSAccessKeyId();

        String host = String.format("git.elasticbeanstalk.%s.amazonaws.com",
                region);

        String path = String.format("/v1/repos/%s/commitid/%s",
                hexEncode(applicationId), hexEncode(commitId));

        if (isNotBlank(environmentName)) {
            path += String.format("/environment/%s", hexEncode(environmentName));
        }

        String scope = String.format("%s/%s/%s/%s", strDate,
                region, service, TERMINATOR);

        StringBuilder stringToSign = new StringBuilder();

        stringToSign.append(String.format("%s-%s\n%s\n%s\n", SCHEME,
                AWS_ALGORITHM, strDateTime, scope));

        stringToSign.append(DigestUtils.sha256Hex(String.format(
                "GIT\n%s\n\nhost:%s\n\nhost\n", path, host).getBytes()));

        byte[] key = deriveKey();

        byte[] digest = hash(key, stringToSign.toString());

        String signature = Hex.encodeHexString(digest);

        String password = strDateTime.concat("Z").concat(signature);

        String returnUrl = String.format("https://%s:%s@%s%s", user, password,
                host, path);

        return returnUrl;
    }
}

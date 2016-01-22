package br.com.ingenieux.mojo.beanstalk.bundle;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;
import java.util.SimpleTimeZone;

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

package com.precisionhawk.poleams.support.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

/**
 *
 * @author pchapman
 */
public abstract class AwsClientFactory {
    
    private String accessKey;
    public String getAccessKey() {
        return accessKey;
    }
//    @Value("${aws.access}")
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
    
    private String region;
    public String getRegion() {
        return region;
    }
//    @Value("${aws.region}")
    public void setRegion(String region) {
        this.region = region;
    }
    
    private String secretKey;
    public String getSecretKey() {
        return secretKey;
    }
//    @Value("${aws.secret}")
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    private AWSCredentialsProvider credentialsProvider;
    public AWSCredentialsProvider createCredentials() {
        synchronized (this) {
            if (credentialsProvider == null) {
                credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAccessKey(), getSecretKey()));                
            }
        }
        return credentialsProvider;
    }
}

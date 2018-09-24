package com.precisionhawk.poleams.support.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import javax.inject.Inject;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class AwsClientFactory {
    
    @Inject private AwsConfig config;
    
    protected String getAccessKey() {
        return config.getAccessKey();
    }
    
    protected String getRegion() {
        return config.getRegion();
    }
    
    public String getSecretKey() {
        return config.getSecretKey();
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

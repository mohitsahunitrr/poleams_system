package com.precisionhawk.poleams.support.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import javax.inject.Inject;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class AwsClientFactory {
    
    private static final Object LOCK = new Object();
    
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
        synchronized (LOCK) {
            if (credentialsProvider == null) {
                credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAccessKey(), getSecretKey()));
            }
        }
        return credentialsProvider;
    }
}

class AWSStaticCredentialsProvider implements AWSCredentialsProvider {
    
    private final AWSCredentials credentials;
    AWSStaticCredentialsProvider(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public AWSCredentials getCredentials() {
        return credentials;
    }

    @Override
    public void refresh() {}
    
}
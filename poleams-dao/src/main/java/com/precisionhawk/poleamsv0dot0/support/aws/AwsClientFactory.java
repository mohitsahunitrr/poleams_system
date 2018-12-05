package com.precisionhawk.poleamsv0dot0.support.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class AwsClientFactory {
    
    private static final Object LOCK = new Object();
    protected static final Logger LOGGER = LoggerFactory.getLogger(AwsClientFactory.class);
    
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
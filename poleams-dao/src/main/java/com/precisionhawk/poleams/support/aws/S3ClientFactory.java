package com.precisionhawk.poleams.support.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author pchapman
 */
@Named
public class S3ClientFactory extends AwsClientFactory implements Provider<AmazonS3> {
    
    private AmazonS3 client;
    
    public AmazonS3 get() {
        synchronized (this) {
            if (client == null) {
                AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
                builder.setCredentials(createCredentials());
                if (getRegion() != null && getRegion().length() > 0) {
                    builder.setRegion(getRegion());
                }
                client = builder.build();
            }
        }
        return client;
    }
}

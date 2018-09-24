package com.precisionhawk.poleams.support.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import javax.inject.Provider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import javax.inject.Named;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class DynamoDBClientFactory extends AwsClientFactory implements Provider<AmazonDynamoDB> {
    
    private static final Object LOCK = new Object();

    private String endpointURL;

    private AmazonDynamoDB client;
    
    @Override
    public AmazonDynamoDB get() {
        synchronized (LOCK) {
            if (client == null) {
                AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
                        .withCredentials(createCredentials());
                if (endpointURL != null && endpointURL.length() > 0) {
                    builder = builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointURL, "us-west-2"));
                } else if (getRegion() != null && getRegion().length() > 0) {
                    builder = builder.withRegion(getRegion());
                }
                client = builder.build();
            }
        }
        return client;
    }
}

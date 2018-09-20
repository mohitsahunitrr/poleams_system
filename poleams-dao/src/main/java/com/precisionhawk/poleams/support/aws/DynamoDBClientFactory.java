package com.precisionhawk.poleams.support.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import javax.inject.Provider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class DynamoDBClientFactory extends AwsClientFactory implements Provider<AmazonDynamoDB> {
    
    private String endpointURL;

    private AmazonDynamoDB client;
    
    @Override
    public AmazonDynamoDB get() {
        synchronized (this) {
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

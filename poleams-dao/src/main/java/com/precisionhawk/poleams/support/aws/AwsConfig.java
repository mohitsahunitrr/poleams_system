package com.precisionhawk.poleams.support.aws;

/**
 *
 * @author pchapman
 */
public interface AwsConfig {
    
    public String getAccessKey();
    
    public String getBucketName();
    
    public String getRegion();
    
    public String getSecretKey();
}

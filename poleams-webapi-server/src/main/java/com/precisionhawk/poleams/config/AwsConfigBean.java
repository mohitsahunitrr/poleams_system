package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.support.aws.AwsConfig;

/**
 *
 * @author Philip A. Chapman
 */
public class AwsConfigBean implements AwsConfig {

    private String accessKey;
    @Override
    public String getAccessKey() {
        return accessKey;
    }
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    private String region;
    @Override
    public String getRegion() {
        return region;
    }
    public void setRegion(String region) {
        this.region = region;
    }

    private String secretKey;
    @Override
    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

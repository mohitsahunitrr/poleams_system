package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.support.aws.AwsConfig;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class AwsConfigFactory implements Provider<AwsConfig> {
    
    @Inject private AppConfig appConfig;
    
    @Override
    public AwsConfig get() {
        return appConfig.getAwsConfig();
    }
}

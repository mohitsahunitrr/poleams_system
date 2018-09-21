package com.precisionhawk.poleams.config;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author pchapman
 */
@Named
public class ServicesConfigFactory implements Provider<ServicesConfig> {
    
    @Inject AppConfig appConfig;

    @Override
    public ServicesConfig get() {
        return appConfig.getServicesConfig();
    }
}

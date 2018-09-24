package com.precisionhawk.poleams.config;

import com.precisionhawk.poleams.repository.RepositoryConfig;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class RepositoryConfigFactory implements Provider<RepositoryConfig> {
    
    @Inject private AppConfig appConfig;

    @Override
    public RepositoryConfig get() {
        return appConfig.getRepositoryConfig();
    }
    
}

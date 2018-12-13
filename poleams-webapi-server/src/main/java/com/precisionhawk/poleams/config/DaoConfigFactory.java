package com.precisionhawk.poleams.config;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author pchapman
 */
@Named("DaoConfigMap")
public class DaoConfigFactory implements Provider<Map<String, Object>> {
    @Inject private AppConfig appConfig;
    
    @Override
    public Map<String, Object> get() {
        return appConfig.getDaoConfig();
    }
}

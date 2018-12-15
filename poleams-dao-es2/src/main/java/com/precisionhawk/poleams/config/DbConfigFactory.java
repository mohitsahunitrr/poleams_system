package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.DbConfig;
import com.precisionhawk.ams.config.MapToConfig;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author pchapman
 */
public class DbConfigFactory extends MapToConfig implements Provider<DbConfig> {

    @Inject
    @Named("DaoConfigMap")
    private Map<String, Object> configMap;
    public Map<String, Object> getConfigMap() {
        return configMap;
    }
    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }
    
    private DbConfigBean config;
    
    private final Object LOCK = new Object();
    
    @Override
    protected Map<String, Object> configMap() {
        return configMap;
    }
    
    @Override
    public DbConfig get() {
        synchronized (LOCK) {
            if (config == null) {
                config = new DbConfigBean();
                config.setDataBaseName(stringFromMap("dataBaseName"));
                config.setInitialConnections(integerFromMap("initialConnections"));
                config.setMaxConnections(integerFromMap("maxConnections"));
                config.setPassword(stringFromMap("password"));
                config.setPortNumber(integerFromMap("portNumber"));
                config.setServerName(stringFromMap("serverName"));
                config.setUserName(stringFromMap("userName"));
            }
        }
        return config;
    }
}

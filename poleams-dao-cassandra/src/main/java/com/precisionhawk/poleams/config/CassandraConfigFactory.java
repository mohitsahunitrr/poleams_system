package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.MapToConfig;
import com.precisionhawk.ams.support.cassandra.CassandraConfig;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author pchapman
 */
public class CassandraConfigFactory extends MapToConfig implements Provider<CassandraConfig> {

    @Inject
    @Named("DaoConfigMap")
    private Map<String, Object> configMap;
    public Map<String, Object> getConfigMap() {
        return configMap;
    }
    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }
    
    private CassandraConfigBean config;
    
    private final Object LOCK = new Object();
    
    @Override
    protected Map<String, Object> configMap() {
        return configMap;
    }

    @Override
    public CassandraConfig get() {
        synchronized (LOCK) {
            config = new CassandraConfigBean();
            config.setKeyspace(stringFromMap("keyspace"));
            config.setNodeHosts(stringFromMap("nodeHosts"));
        }
        return config;
    }
    
}

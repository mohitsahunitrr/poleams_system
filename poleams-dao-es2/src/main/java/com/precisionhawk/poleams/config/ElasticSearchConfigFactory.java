package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.MapToConfig;
import com.precisionhawk.ams.support.elasticsearch.ElasticSearchConfig;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class ElasticSearchConfigFactory extends MapToConfig implements Provider<ElasticSearchConfig> {

    @Inject
    @Named("DaoConfigMap")
    private Map<String, Object> configMap;
    public Map<String, Object> getConfigMap() {
        return configMap;
    }
    public void setConfigMap(Map<String, Object> configMap) {
        this.configMap = configMap;
    }
    
    private ElasticSearchConfigBean config;
    
    private final Object LOCK = new Object();
    
    @Override
    protected Map<String, Object> configMap() {
        return configMap;
    }
    
    @Override
    public ElasticSearchConfig get() {
        synchronized (LOCK) {
            if (config == null) {
                config = new ElasticSearchConfigBean();
                config.setBulkSize(integerFromMap("bulkSize"));
                config.setClusterName(stringFromMap("clusterName"));
                config.setConnectTimeout(stringFromMap("connectTimeout"));
                config.setInProcess(booleanFromMap("inProcess"));
                config.setNodeHosts(stringFromMap("nodeHosts"));
                config.setScrollLifespan(longFromMap("scrollLifespan"));
                config.setScrollSize(integerFromMap("scrollSize"));
            }
        }
        return config;
    }
}

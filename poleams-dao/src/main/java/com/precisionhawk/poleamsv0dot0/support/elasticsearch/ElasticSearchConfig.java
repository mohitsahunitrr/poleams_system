package com.precisionhawk.poleamsv0dot0.support.elasticsearch;

/**
 * Implemented by a class that provides configuration for DAOs making use of
 * ElasticSearch.
 *
 * @author Philip A. Chapman
 */
public interface ElasticSearchConfig {

    public Integer getBulkSize();
    
    public String getClusterName();
    
    public String getConnectTimeout();
    
    public Boolean isInProcess();
    
    public String getNodeHosts();

    public Long getScrollLifespan();

    public Integer getScrollSize();
    
}

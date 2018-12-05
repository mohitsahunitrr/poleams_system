package com.precisionhawk.poleamsv0dot0.convert;

import com.precisionhawk.ams.support.elasticsearch.ElasticSearchConfig;

/**
 *
 * @author pchapman
 */
public class ElasticSearchConfigBean implements ElasticSearchConfig {

    @Override
    public Integer getBulkSize() {
        return 100;
    }

    private String clusterName;
    @Override
    public String getClusterName() {
        return clusterName;
    }
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public String getConnectTimeout() {
        return "60s";
    }

    @Override
    public Boolean isInProcess() {
        return false;
    }

    private String nodeHosts;
    @Override
    public String getNodeHosts() {
        return nodeHosts;
    }
    public void setNodeHosts(String nodeHosts) {
        this.nodeHosts = nodeHosts;
    }

    @Override
    public Long getScrollLifespan() {
        return 60000L;
    }

    @Override
    public Integer getScrollSize() {
        return 100;
    }

    boolean isValid() {
        return clusterName != null && (!clusterName.isEmpty())
                && nodeHosts != null && (!nodeHosts.isEmpty());
    }
}

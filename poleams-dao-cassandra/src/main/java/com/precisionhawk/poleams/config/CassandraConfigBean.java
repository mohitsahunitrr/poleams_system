package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.support.cassandra.CassandraConfig;

/**
 *
 * @author pchapman
 */
public class CassandraConfigBean implements CassandraConfig {

    private String keyspace;
    @Override
    public String getKeyspace() {
        return keyspace;
    }
    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    private String nodeHosts;
    @Override
    public String getNodeHosts() {
        return nodeHosts;
    }
    public void setNodeHosts(String nodeHosts) {
        this.nodeHosts = nodeHosts;
    }
    
}

package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.DbConfig;

/**
 *
 * @author pchapman
 */
public class DbConfigBean implements DbConfig {
        private String dataBaseName;
    private String password;
    private Integer portNumber;
    private String serverName;
    private String userName;
    private Integer initialConnections;
    private Integer maxConnections;

    @Override
    public String getDataBaseName() {
        return dataBaseName;
    }
    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }

    @Override
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Integer getPortNumber() {
        return portNumber;
    }
    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public String getServerName() {
        return serverName;
    }
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public Integer getInitialConnections() {
        return initialConnections;
    }
    public void setInitialConnections(Integer initialConnections) {
        this.initialConnections = initialConnections;
    }

    @Override
    public Integer getMaxConnections() {
        return maxConnections;
    }
    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }
}

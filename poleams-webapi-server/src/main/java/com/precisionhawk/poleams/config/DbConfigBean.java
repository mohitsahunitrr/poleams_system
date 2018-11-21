package com.precisionhawk.poleams.config;

/**
 *
 * @author pchapman
 */
public class DbConfigBean {
    
    private String dataBaseName;
    private String password;
    private Integer portNumber;
    private String serverName;
    private String userName;
    private Integer initialConnections;
    private Integer maxCommections;

    public String getDataBaseName() {
        return dataBaseName;
    }

    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getInitialConnections() {
        return initialConnections;
    }

    public void setInitialConnections(Integer initialConnections) {
        this.initialConnections = initialConnections;
    }

    public Integer getMaxCommections() {
        return maxCommections;
    }

    public void setMaxCommections(Integer maxCommections) {
        this.maxCommections = maxCommections;
    }
    
    
}

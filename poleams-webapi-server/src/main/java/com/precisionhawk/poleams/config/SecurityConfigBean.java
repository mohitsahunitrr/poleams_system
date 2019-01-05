package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.ClientConfig;
import com.precisionhawk.ams.config.SecurityConfig;
import com.precisionhawk.ams.config.TenantConfig;
import com.precisionhawk.ams.dao.SecurityDaoConfig;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class SecurityConfigBean implements SecurityDaoConfig, SecurityConfig {
    
    private String securityImplementation;
    @Override
    public String getSecurityImplementation() {
        return securityImplementation;
    }
    public void setSecurityImplementation(String impl) {
        this.securityImplementation = impl;
    }
    
    private Long userInfoCacheTime;
    @Override
    public Long getUserInfoCacheTime() {
        return userInfoCacheTime;
    }
    public void setUserInfoCacheTime(Long time) {
        this.userInfoCacheTime = time;
    }

    /**
     * Configurations for this service within Azure as a registered application mapped
     * by tenant ID.  The application can be registered with multiple Azure tenants
     * allowing for authentication against each of them.
     */
    private Map<String, TenantConfig> tenantConfigurations = new HashMap<>();
    @Override
    public Map<String, TenantConfig> getTenantConfigurations() {
        return tenantConfigurations;
    }
    public void setTenantConfigurations(Map<String, TenantConfig> tenantConfigurations) {
        this.tenantConfigurations = tenantConfigurations;
    }
    
    /**
     * Configurations for clients which may programmatically access these services mapped
     * by Azure Client ID.  This allows for server-to-server communication.
     */
    private Map<String, ClientConfig> clientConfigurations = new HashMap<>();
    @Override
    public Map<String, ClientConfig> getClientConfigurations() {
        return clientConfigurations;
    }
    public void setClientConfigurations(Map<String, ClientConfig> clientConfigurations) {
        this.clientConfigurations = clientConfigurations;
    }

    private String securityDataFile;
    @Override
    public String getSecurityDataFile() {
        return securityDataFile;
    }
    public void setSecurityDataFile(String userDatasFile) {
        this.securityDataFile = userDatasFile;
    }

    private String securityDaoImpl;
    @Override
    public String getSecurityDaoImplementation() {
        return securityDaoImpl;
    }
    public void setSecurityDaoImplementation(String securityDaoImpl) {
        this.securityDaoImpl = securityDaoImpl;
    }
}

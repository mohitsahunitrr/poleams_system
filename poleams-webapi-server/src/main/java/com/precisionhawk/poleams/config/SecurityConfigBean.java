package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.ClientConfig;
import com.precisionhawk.ams.config.SecurityConfig;
import com.precisionhawk.ams.config.TenantConfig;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public class SecurityConfigBean implements SecurityConfig {
    
    private String securityImplementation;
    @Override
    public String getSecurityImplementation() {
        return securityImplementation;
    }
    public void setSecurityImplementation(String impl) {
        this.securityImplementation = impl;
    }
    
    private Integer maxRetries;
    @Override
    public Integer getMaxRetries() {
        return maxRetries;
    }
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
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
}

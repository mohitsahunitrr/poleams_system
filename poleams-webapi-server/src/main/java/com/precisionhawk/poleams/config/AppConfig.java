package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.NotificationServicesConfigBean;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Philip A. Chapman
 */
public class AppConfig {
    
    private Map<String, Object> awsConfig;
    public Map<String, Object> getAwsConfig() {
        return awsConfig;
    }
    public void setAwsConfig(Map<String, Object> awsConfig) {
        this.awsConfig = awsConfig;
    }
    
    private CacheConfigBean cacheConfig;
    public CacheConfigBean getCacheConfig() {
        return cacheConfig;
    }
    public void setCacheConfig(CacheConfigBean cacheConfig) {
        this.cacheConfig = cacheConfig;
    }
    
    private Map<String, Object> daoConfig = new HashMap<>();
    public Map<String, Object> getDaoConfig() {
        return daoConfig;
    }
    public void setDaoConfig(Map<String, Object> daoConfig) {
        this.daoConfig = daoConfig;
    }

    private NotificationServicesConfigBean notificationServicesConfig;
    public NotificationServicesConfigBean getNotificationServicesConfig() {
        return notificationServicesConfig;
    }
    public void setNotificationServicesConfig(NotificationServicesConfigBean notificationServicesConfig) {
        this.notificationServicesConfig = notificationServicesConfig;
    }
    
    private RepositoryConfigBean repositoryConfig;
    public RepositoryConfigBean getRepositoryConfig() {
        return repositoryConfig;
    }
    public void setRepositoryConfig(RepositoryConfigBean repositoryConfig) {
        this.repositoryConfig = repositoryConfig;
    }
    
    private SecurityConfigBean securityConfig;
    public SecurityConfigBean getSecurityConfig() {
        return securityConfig;
    }
    public void setSecurityConfig(SecurityConfigBean config) {
        this.securityConfig = config;
    }

    private ServicesConfigBean servicesConfig;
    public ServicesConfigBean getServicesConfig() {
        return servicesConfig;
    }
    public void setServicesConfig(ServicesConfigBean servicesConfig) {
        this.servicesConfig = servicesConfig;
    }
}

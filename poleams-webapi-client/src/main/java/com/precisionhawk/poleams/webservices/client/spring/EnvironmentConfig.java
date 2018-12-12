/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.client.spring;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class EnvironmentConfig {

    private String accessTokenProvider;
    public String getAccessTokenProvider() {
        return accessTokenProvider;
    }
    public void setAccessTokenProvider(String accessTokenProvider) {
        this.accessTokenProvider = accessTokenProvider;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    private String serviceURI;
    public String getServiceURI() {
        return serviceURI;
    }
    public void setServiceURI(String serviceURI) {
        this.serviceURI = serviceURI;
    }
    
    private String clientId;
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    private String clientSecret;
    public String getClientSecret() {
        return clientSecret;
    }
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    private String tenantId;
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    private String serviceAppId;
    public String getServiceAppId() {
        return serviceAppId;
    }
    public void setServiceAppId(String serviceAppId) {
        this.serviceAppId = serviceAppId;
    }
}

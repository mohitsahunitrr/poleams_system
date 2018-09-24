package com.precisionhawk.poleams.config;

/**
 *
 * @author Philip A. Chapman
 */
public class ServicesConfigBean implements ServicesConfig {
    
    private String appName;
    @Override
    public String getAppName() {
        return appName;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }

    private Boolean enforceSSL;
    @Override
    public Boolean getEnforceSSL() {
        return enforceSSL;
    }
    public void setEnforceSSL(Boolean enforceSSL) {
        this.enforceSSL = enforceSSL;
    }

    private String environment;
    @Override
    public String getEnvironment() {
        return environment;
    }
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    private String loggingConfigURI;
    @Override
    public String getLoggingConfigURI() {
        return loggingConfigURI;
    }
    public void setLoggingConfigURI(String loggingConfigURI) {
        this.loggingConfigURI = loggingConfigURI;
    }
    
    private String servicesURL;
    @Override
    public String getServicesURL() {
        return servicesURL;
    }
    public void setServicesURL(String servicesURL) {
        this.servicesURL = servicesURL;
    }

    private String version;
    @Override
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    
}

package com.precisionhawk.poleams.config;

/**
 *
 * @author Philip A. Chapman
 */
public interface ServicesConfig {
    
    public String getAppName();
    
    public String getEnvironment();
    
    /** If non-SSL traffic should be blocked. */
    public Boolean getEnforceSSL();
    
    public String getLoggingConfigURI();
    
    /** The URL for services.  This is used for generating download URLs for resources. */
    public String getServicesURL();
    
    public String getUsersListFile();
    
    public String getVersion();
}

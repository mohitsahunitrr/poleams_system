package com.precisionhawk.poleams.config;

/**
 *
 * @author Philip A. Chapman
 */
public class AppConfig {
    
    private AwsConfigBean awsConfig;
    public AwsConfigBean getAwsConfig() {
        return awsConfig;
    }
    public void setAwsConfig(AwsConfigBean awsConfig) {
        this.awsConfig = awsConfig;
    }
    
    private CacheConfigBean cacheConfig;
    public CacheConfigBean getCacheConfig() {
        return cacheConfig;
    }
    public void setCacheConfig(CacheConfigBean cacheConfig) {
        this.cacheConfig = cacheConfig;
    }
    
    private DbConfigBean dbConfig;
    public DbConfigBean getDbConfig() {
        return dbConfig;
    }
    public void setDbConfig(DbConfigBean config) {
        this.dbConfig = config;
    }

    private ElasticSearchConfigBean elasticSearchConfig;
    public ElasticSearchConfigBean getElasticSearchConfig() {
        return elasticSearchConfig;
    }
    public void setElasticSearchConfig(ElasticSearchConfigBean elasticSearchConfig) {
        this.elasticSearchConfig = elasticSearchConfig;
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

package com.precisionhawk.poleams.config;

/**
 *
 * @author pchapman
 */
public class AppConfig {
    
    private AwsConfigBean awsConfig;
    public AwsConfigBean getAwsConfig() {
        return awsConfig;
    }
    public void setAwsConfig(AwsConfigBean awsConfig) {
        this.awsConfig = awsConfig;
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

    private ServicesConfigBean servicesConfig;
    public ServicesConfigBean getServicesConfig() {
        return servicesConfig;
    }
    public void setServicesConfig(ServicesConfigBean servicesConfig) {
        this.servicesConfig = servicesConfig;
    }
}

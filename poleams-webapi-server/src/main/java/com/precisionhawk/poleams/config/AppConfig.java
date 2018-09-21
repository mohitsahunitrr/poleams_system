package com.precisionhawk.poleams.config;

/**
 *
 * @author pchapman
 */
public class AppConfig {
    
    private AwsConfigBean awsConfigBean;
    public AwsConfigBean getAwsConfigBean() {
        return awsConfigBean;
    }
    public void setAwsConfigBean(AwsConfigBean awsConfigBean) {
        this.awsConfigBean = awsConfigBean;
    }

    private ElasticSearchConfigBean elasticSearchConfigBean;
    public ElasticSearchConfigBean getElasticSearchConfigBean() {
        return elasticSearchConfigBean;
    }
    public void setElasticSearchConfigBean(ElasticSearchConfigBean elasticSearchConfigBean) {
        this.elasticSearchConfigBean = elasticSearchConfigBean;
    }

    private RepositoryConfigBean repositoryConfigBean;
    public RepositoryConfigBean getRepositoryConfigBean() {
        return repositoryConfigBean;
    }
    public void setRepositoryConfigBean(RepositoryConfigBean repositoryConfigBean) {
        this.repositoryConfigBean = repositoryConfigBean;
    }

    private ServicesConfigBean servicesConfigBean;
    public ServicesConfigBean getServicesConfigBean() {
        return servicesConfigBean;
    }
    public void setServicesConfigBean(ServicesConfigBean bean) {
        this.servicesConfigBean = bean;
    }
}

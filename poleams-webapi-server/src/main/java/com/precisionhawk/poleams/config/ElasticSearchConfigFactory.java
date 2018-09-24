/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.precisionhawk.poleams.config;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConfig;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class ElasticSearchConfigFactory implements Provider<ElasticSearchConfig> {

    @Inject private AppConfig appConfig;
    
    @Override
    public ElasticSearchConfig get() {
        return appConfig.getElasticSearchConfig();
    }
    
}

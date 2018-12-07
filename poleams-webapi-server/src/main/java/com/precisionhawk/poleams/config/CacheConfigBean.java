package com.precisionhawk.poleams.config;

import com.precisionhawk.ams.config.CacheConfig;

/**
 *
 * @author pchapman
 */
public class CacheConfigBean implements CacheConfig {

    private String impl;
    @Override
    public String getCacheImplementation() {
        return impl;
    }
    public void setCacheImplementation(String impl) {
        this.impl = impl;
    }

    private Integer timeout;
    @Override
    public Integer getTimeout() {
        return timeout;
    }
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}

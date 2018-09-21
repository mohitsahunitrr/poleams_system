/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.client;

import com.precisionhawk.poleams.webservices.WebService;
import com.precisionhawk.poleams.webservices.client.spring.WebServicesFactory;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Privates the means for accessing images in an environment for processing.
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class Environment {

//    private AccessTokenProvider accessTokenProvider;
//    public void setAccessTokenProvider(AccessTokenProvider accessTokenProvider) {
//        this.accessTokenProvider = accessTokenProvider;
//    }
//
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    private String serviceAppId;
    public void setServiceAppId(String serviceAppId) {
        this.serviceAppId = serviceAppId;
    }
    
    private WebServicesFactory servicesFactory;
    public void setServicesFactory(WebServicesFactory servicesFactory) {
        this.servicesFactory = servicesFactory;
    }

    /**
     * Obtains an access token which can be used to call services in this
     * environment.  This access token has a lifespan and should not be cached.
     * Caching of access tokens is done internally.
     * @return The access token.
     * @throws IOException Indicates an error communicating with the back-end
     *                     authorization services.
     */
    public String obtainAccessToken() throws IOException {
//        return accessTokenProvider.obtainAccessToken(serviceAppId);
        return "NotImplemented";
    }
    
    private final Map<Class<? extends WebService>, WebService> services = new HashMap();
    public <T extends WebService> T obtainWebService(Class<T> clazz) {
        synchronized (services) {
            T service = (T)services.get(clazz);
            if (service == null) {
                service = servicesFactory.buildWebService((Class<T>)clazz);
                services.put(clazz, service);
            }
            return service;
        }
    }
    
    public URI getServiceURI() {
        return servicesFactory.getServiceURI();
    }
}

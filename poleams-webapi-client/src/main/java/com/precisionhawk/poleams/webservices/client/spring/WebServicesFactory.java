/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.client.spring;

import com.precisionhawk.poleams.webservices.WebService;
import java.net.URI;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class WebServicesFactory
{
    private ClientExecutor clientExecutor;
    private ResteasyProviderFactory resteasyProviderFactory;
    private URI serviceURI;

    WebServicesFactory(URI serviceURI, HttpClient httpClient, ClientExecutor clientExecutor, ResteasyProviderFactory resteasyProviderFactory) {
        this.serviceURI = serviceURI;
        if (resteasyProviderFactory == null) {
            this.resteasyProviderFactory = ResteasyProviderFactory.getInstance();
        } else {
            this.resteasyProviderFactory = resteasyProviderFactory;
        }
        RegisterBuiltin.register(resteasyProviderFactory);
        if (clientExecutor == null) {
            if (httpClient == null) {
                httpClient = HttpClientBuilder.create().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
            }
            this.clientExecutor = new ApacheHttpClient4Executor(httpClient);
        } else {
            this.clientExecutor = clientExecutor;
        }
    }
    
    public <T extends WebService> T buildWebService(Class<T> clazz) {
        return ProxyFactory.create(clazz, serviceURI, clientExecutor, resteasyProviderFactory);
    }
    
    public URI getServiceURI() {
        return serviceURI;
    }
}

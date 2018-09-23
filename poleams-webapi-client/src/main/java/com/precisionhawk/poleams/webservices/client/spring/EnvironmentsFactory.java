/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.client.spring;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.precisionhawk.poleams.support.resteasy.JacksonProvider;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
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
public class EnvironmentsFactory
{
    private String configFilePath;
    private HttpClient httpClient;
    private ClientExecutor clientExecutor;
    private ResteasyProviderFactory resteasyProviderFactory;    
    private List<Environment> environments;
    
    /** The path to the JSON file from which a list of EnvironmentConfig objects will be parsed. */
    public String getConfigFilePath() {
        return configFilePath;
    }
    /** The path to the JSON file from which a list of EnvironmentConfig objects will be parsed. */
    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public List<Environment> getEnvironments() throws Exception {
        return environments;
    }

    public void init() throws Exception {
        
        List<EnvironmentConfig> configs;
        Reader reader = null;
        try {
            reader = new InputStreamReader(configURL().openStream());
            YamlReader yamlreader = new YamlReader(reader);
            configs = yamlreader.read(List.class, EnvironmentConfig.class);
        } finally {
            IOUtils.closeQuietly(reader);
        }
        
        // Set up restful client stuff
        if (resteasyProviderFactory == null) {
            resteasyProviderFactory = ResteasyProviderFactory.getInstance();
        }
        RegisterBuiltin.register(resteasyProviderFactory);
        if (clientExecutor == null) {
            if (httpClient == null) {
                httpClient = HttpClientBuilder.create().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
            }
            clientExecutor = new ApacheHttpClient4Executor(httpClient);
        }
        
        environments = new LinkedList<>();
        for (EnvironmentConfig config : configs) {
            Environment env = new Environment();
//            AccessTokenProvider tokenProvider = new AccessTokenProvider();
//            tokenProvider.setClientId(config.getClientId());
//            tokenProvider.setClientSecret(config.getClientSecret());
//            tokenProvider.setTenantId(config.getTenantId());
//            env.setAccessTokenProvider(tokenProvider);
            env.setName(config.getName());
            env.setServiceAppId(config.getServiceAppId());
            URI svcuri = new URI(config.getServiceURI());
            WebServicesFactory servicesFactory = new WebServicesFactory(svcuri, httpClient, clientExecutor, resteasyProviderFactory);
            env.setServicesFactory(servicesFactory);
            environments.add(env);
        }
    }


    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Optional property. If this property is set and {@link #clientExecutor} is
     * null, this will be used by proxy generation. This could be useful for
     * example when you want to use a
     * {@link org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager}
     * instead of a {@link org.apache.http.impl.conn.SingleClientConnManager}
     * which is the default in {@link org.apache.http.client.HttpClient}.
     *
     * @param httpClient the instance to be used by proxy generation
     * @see ProxyFactory#create(Class, URI, HttpClient, ResteasyProviderFactory)
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ClientExecutor getClientExecutor() {
        return clientExecutor;
    }

    /**
     * Optional property for advanced usage. If this property is set it will be
     * used by proxy generation. If this property is set the {@link #httpClient}
     * property is ignored.
     *
     * @param clientExecutor the instance to be used by proxy generation
     * @see ProxyFactory#create(Class, URI, ClientExecutor,
     * ResteasyProviderFactory)
     */
    public void setClientExecutor(ClientExecutor clientExecutor) {
        this.clientExecutor = clientExecutor;
    }

    public ResteasyProviderFactory getResteasyProviderFactory() {
        return resteasyProviderFactory;
    }

    /**
     * Optional property for advanced usage. For the most cases this property is
     * not needed to be set.
     *
     * @param resteasyProviderFactory the instance to be used by proxy
     * generation.
     */
    public void setResteasyProviderFactory(
            ResteasyProviderFactory resteasyProviderFactory) {
        this.resteasyProviderFactory = resteasyProviderFactory;
    }
    
    protected URL configURL() throws MalformedURLException {
        String urlString = getConfigFilePath();
        if (urlString == null || urlString.length() == 0) {
            throw new IllegalArgumentException("Processor environments file path not set or invalid.");
        }
        
        URL url;
        if (urlString.startsWith("classpath:")) {
            urlString = urlString.substring(10);
            url = getClass().getClassLoader().getResource(urlString);
            if (url == null) {
                throw new IllegalArgumentException("Log4J configuration XML not found on classpath at " + urlString);
            }
        } else {
            url = new URL(urlString);
        }
        return url;
    }
}

/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.webservices.AbstractWebService;
import com.precisionhawk.poleams.config.ServicesConfig;
import com.precisionhawk.ams.repository.RepositoryConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import com.precisionhawk.poleams.webservices.StatusWebService;
import java.time.ZonedDateTime;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Named
public class StatusWebServiceImpl extends AbstractWebService implements StatusWebService {
    
    @Inject private RepositoryConfig repoConfig;
    @Inject private ServicesConfig svcsConfig;

    private final Map<String, Object> statusMap = new LinkedHashMap<>();
    
    @Override
    public Map<String, Object> retrieveStatus() {
        synchronized (statusMap) {
            if (statusMap.isEmpty()) {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("name", svcsConfig.getAppName());
                data.put("environment", svcsConfig.getEnvironment());
                data.put("version", svcsConfig.getVersion());
                statusMap.put("application", data);
                data = new LinkedHashMap<>();
                data.put("resourceRepository", repoConfig.getRepositoryImplementation());
                statusMap.put("implementations", data);
            }
        }
        statusMap.put("currentTime", ZonedDateTime.now());
        return statusMap;
    }
}

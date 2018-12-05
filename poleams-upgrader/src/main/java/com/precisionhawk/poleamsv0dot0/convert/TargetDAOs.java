package com.precisionhawk.poleamsv0dot0.convert;

import com.precisionhawk.ams.dao.ResourceMetadataDao;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.ams.support.elasticsearch.ElasticSearchClientFactory;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.poleams.dao.*;
import com.precisionhawk.poleams.dao.elasticsearch.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.client.Client;
import us.pcsw.es.util.ClientLifecycleListener;

/**
 *
 * @author pchapman
 */
public class TargetDAOs {

    private final FeederEsDao feederDao;
    private final FeederInspectionEsDao feederInspectionDao;
    private final PoleEsDao poleDao;
    private final PoleInspectionEsDao poleInspectionDao;
    private final ResourceMetadataEsDao resourceMetadataDao;
    
    public TargetDAOs(ElasticSearchConfigBean config) {
        ElasticSearchClientFactory factory = new ElasticSearchClientFactory();
        factory.setConfig(config);
        factory.setListener(new ClientLifecycleListener(){
            @Override
            public void clientConnectionOpened(Client client) {}

            @Override
            public void prepareForClose(Client client) {}

            @Override
            public void afterClose() {}
        });
        Client client = factory.get();
        ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
        feederDao = new FeederEsDao();
        config(feederDao, config, client, mapper);
        feederInspectionDao = new FeederInspectionEsDao();
        config(feederInspectionDao, config, client, mapper);
        poleDao = new PoleEsDao();
        config(poleDao, config, client, mapper);
        poleInspectionDao = new PoleInspectionEsDao();
        config(poleInspectionDao, config, client, mapper);
        resourceMetadataDao = new ResourceMetadataEsDao();
        config(resourceMetadataDao, config, client, mapper);
    }
    
    private void config(AbstractEsDao dao, ElasticSearchConfigBean config, Client client, ObjectMapper mapper) {
        dao.setClient(client);
        dao.setConfig(config);
        dao.setMapper(mapper);
        dao.init();
    }

    public FeederDao getFeederDao() {
        return feederDao;
    }

    public FeederInspectionDao getFeederInspectionDao() {
        return feederInspectionDao;
    }

    public PoleDao getPoleDao() {
        return poleDao;
    }

    public PoleInspectionDao getPoleInspectionDao() {
        return poleInspectionDao;
    }

    public ResourceMetadataDao getResourceMetadataDao() {
        return resourceMetadataDao;
    }
}

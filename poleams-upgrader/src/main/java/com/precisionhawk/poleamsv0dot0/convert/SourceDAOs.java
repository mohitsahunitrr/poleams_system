package com.precisionhawk.poleamsv0dot0.convert;

import com.precisionhawk.ams.support.elasticsearch.ElasticSearchClientFactory;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.poleamsv0dot0.dao.*;
import com.precisionhawk.poleamsv0dot0.dao.elasticsearch.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.client.Client;
import us.pcsw.es.util.ClientLifecycleListener;

/**
 *
 * @author pchapman
 */
public class SourceDAOs {

    private final PoleEsDao poleDao;
    private final PoleInspectionEsDao poleInspectionDao;
    private final ResourceMetadataEsDao resourceMetadataDao;
    private final SubStationEsDao subStationDao;
    
    public SourceDAOs(ElasticSearchConfigBean config) {
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
        poleDao = new PoleEsDao();
        config(poleDao, config, client, mapper);
        poleInspectionDao = new PoleInspectionEsDao();
        config(poleInspectionDao, config, client, mapper);
        resourceMetadataDao = new ResourceMetadataEsDao();
        config(resourceMetadataDao, config, client, mapper);
        subStationDao = new SubStationEsDao();
        config(subStationDao, config, client, mapper);
    }
    
    private void config(AbstractEsDao dao, ElasticSearchConfigBean config, Client client, ObjectMapper mapper) {
        dao.setClient(client);
        dao.setConfig(config);
        dao.setMapper(mapper);
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

    public SubStationDao getSubStationDao() {
        return subStationDao;
    }
}
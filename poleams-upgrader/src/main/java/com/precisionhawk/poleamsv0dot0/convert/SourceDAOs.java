package com.precisionhawk.poleamsv0dot0.convert;

import com.precisionhawk.ams.support.elasticsearch.ElasticSearchClientFactory;
import com.precisionhawk.ams.support.elasticsearch.ElasticSearchConfig;
import com.precisionhawk.poleamsv0dot0.dao.*;

/**
 *
 * @author pchapman
 */
public class SourceDAOs {

    private final PoleDao poleDao;
    private final PoleInspectionDao poleInspectionDao;
    private final ResourceMetadataDao resourceMetadataDao;
    private final SubStationDao subStationDao;
    
    public SourceDAOs(ElasticSearchConfig config) {
        ElasticSearchClientFactory factory = new ElasticSearchClientFactory();
    }
}

package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class WorkOrderEsDao extends com.precisionhawk.ams.dao.elasticsearch.WorkOrderEsDao {

    private static final String MAPPING = "com/precisionhawk/ams/dao/elasticsearch/WorkOrder_Mapping.json";
    
    @Override
    protected String getIndexName() {
        return ElasticSearchConstants.INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getMappingPath() {
        return MAPPING;
    }
    
}
package com.precisionhawk.poleams.dao.elasticsearch;

import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class WorkOrderESDao extends com.precisionhawk.ams.dao.elasticsearch.WorkOrderESDao {
    private static final String MAPPING_FORM_DEF = "com/windams/dao/elasticsearch/WorkOrder_Mapping.json";
    @Override
    protected String getMappingPath() {
        return MAPPING_FORM_DEF;
    }

    //TODO: It would be better if this were a configuration item?
    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }    
}

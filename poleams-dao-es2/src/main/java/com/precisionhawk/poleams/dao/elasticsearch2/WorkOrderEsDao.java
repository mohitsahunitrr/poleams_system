package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class WorkOrderEsDao extends com.precisionhawk.ams.dao.elasticsearch2.WorkOrderEsDao {
    
    @Override
    protected String getIndexName() {
        return ElasticSearchConstants.INDEX_NAME_POLEAMS;
    }
    
}

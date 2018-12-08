package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import javax.inject.Named;

/**
 *
 * @author pchapman
 */
@Named
public class InspectionEventEsDao extends com.precisionhawk.ams.dao.elasticsearch.InspectionEventEsDao {

    @Override
    protected String getIndexName() {
        return ElasticSearchConstants.INDEX_NAME_POLEAMS;
    }
    
}

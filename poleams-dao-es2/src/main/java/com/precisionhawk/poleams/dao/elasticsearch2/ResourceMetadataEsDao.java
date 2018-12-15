package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import javax.inject.Named;

/**
 * Implementation of ResourceMetadataDao which stores metadata in Elasticsearch.
 *
 * @author Philip A. Chapman
 */
@Named
public class ResourceMetadataEsDao extends com.precisionhawk.ams.dao.elasticsearch2.ResourceMetadataEsDao implements ElasticSearchConstants {

    //TODO: It would be better if this were a configuration item?
    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }
}

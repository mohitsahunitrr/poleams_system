package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import javax.inject.Named;

/**
 * Implementation of ResourceMetadataDao which stores metadata in Elasticsearch.
 *
 * @author Philip A. Chapman
 */
@Named
public class ResourceMetadataEsDao extends com.precisionhawk.ams.dao.elasticsearch.ResourceMetadataEsDao implements ElasticSearchConstants {

    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/Resource_Mapping.json";

    //TODO: Could this be done a differenet way?
    @Override
    public String getMappingPath() {
        return MAPPING;
    }

    //TODO: It would be better if this were a configuration item?
    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }
}

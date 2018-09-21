package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.ResourceMetadataDao;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import java.util.List;

import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
//import us.pcsw.es.util.ESUtils;

/**
 * Implementation of ResourceMetadataDao which stores metadata in Elasticsearch.
 *
 * @author pchapman
 */
@Named
public class ResourceMetadataEsDao extends AbstractEsDao implements ResourceMetadataDao {

    private static final String COL_POLE_ID = "poleId";
    private static final String COL_POLE_INSP_ID = "poleInspectionId";
    private static final String COL_STATUS = "status";
    private static final String COL_ORG_ID = "organizationId";
    private static final String COL_CONTENT_TYPE = "contentType";
    private static final String COL_SOURCE_RESOURCE_ID = "sourceResourceId";
    private static final String COL_SUB_STTN_ID = "subStationId";
    private static final String COL_RESOURCE_ID = "resourceId";
    private static final String COL_TYPE = "type";
    private static final String COL_ZOOMIFY_ID = "zoomifyID";
    private static final String DOCUMENT = "Resource";
    
    private static final String MAPPING_RESOURCE_METADATA = "com/precisionhawk/poleams/dao/elasticsearch/Resource_Mapping.json";

//    @PostConstruct
//    public void init() {
//        try {
//            ESUtils.ensureMapping(getClient(), INDEX_NAME_WAREHOUSE, DOCUMENT_TYPE_RESOURCE_METADATA, MAPPING_RESOURCE_METADATA);
//        } catch (Exception e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT;
    }

    @Override
    public ResourceMetadata retrieveResourceMetadata(String resourceId) throws DaoException {
        if (resourceId == null || resourceId.isEmpty()) {
            throw new IllegalArgumentException("Resource ID is required.");
        }
        return retrieveObject(resourceId, ResourceMetadata.class);
    }

    @Override
    public List<ResourceMetadata> lookup(ResourceSearchParameters params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("The search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_ORG_ID, params.getOrganizationId());
        query = addQueryMust(query, COL_POLE_ID, params.getPoleId());
        query = addQueryMust(query, COL_POLE_INSP_ID, params.getPoleInspectionId());
        query = addQueryMust(query, COL_SOURCE_RESOURCE_ID, params.getSourceResourceId());
        query = addQueryMust(query, COL_STATUS, params.getStatus());
        query = addQueryMust(query, COL_SUB_STTN_ID, params.getSubStationId());
        query = addQueryMust(query, COL_TYPE, params.getType());
        query = addQueryMust(query, COL_ZOOMIFY_ID, params.getZoomifyId());
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(INDEX_NAME_POLEAMS)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(ResourceMetadata.class, response, scrollLifeLimit);
    }

    @Override
    public boolean insertMetadata(ResourceMetadata meta) throws DaoException {
        if (meta == null) {
            throw new IllegalArgumentException("The resource metadata is required.");
        } else if (meta.getResourceId() == null || meta.getResourceId().isEmpty()) {
            throw new IllegalArgumentException("The resource ID is required.");
        }
        ResourceMetadata existing = retrieveObject(meta.getResourceId(), ResourceMetadata.class);
        if (existing == null) {
            indexObject(meta.getResourceId(), meta);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean deleteMetadata(String resourceId) throws DaoException {
        if (resourceId == null || resourceId.isEmpty()) {
            throw new IllegalArgumentException("The resource ID is required.");
        }
        deleteDocument(resourceId);
        return true;
    }

    @Override
    public boolean updateMetadata(ResourceMetadata meta) throws DaoException {
        if (meta == null) {
            throw new IllegalArgumentException("The resource metadata is required.");
        } else if (meta.getResourceId() == null || meta.getResourceId().isEmpty()) {
            throw new IllegalArgumentException("The resource ID is required.");
        }
        ResourceMetadata existing = retrieveObject(meta.getResourceId(), ResourceMetadata.class);
        if (existing == null) {
            return false;
        } else {
            indexObject(meta.getResourceId(), meta);
            return true;
        }
    }
}

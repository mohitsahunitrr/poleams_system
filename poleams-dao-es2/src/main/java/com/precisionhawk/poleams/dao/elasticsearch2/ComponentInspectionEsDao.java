package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch2.AbstractEsDao;
import com.precisionhawk.ams.domain.ComponentInspection;
import com.precisionhawk.poleams.dao.ComponentInspectionDao;
import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import java.util.List;
import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class ComponentInspectionEsDao extends AbstractEsDao implements ComponentInspectionDao, ElasticSearchConstants {
    
    private static final String COL_ID = "id";
    private static final String COL_ASSET_ID = "assetId";
    private static final String COL_ASSET_INSPECTION_ID = "assetInspectionId";
    private static final String COL_COMP_ID = "componentId";
    private static final String COL_ORDER_NUM = "orderNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_SITE_INSP_ID = "siteInspectionId";
    private static final String COL_STATUS = "status";
    private static final String COL_TYPE = "type";
    private static final String DOCUMENT = "ComponentInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch2/ComponentInspection_Mapping.json";

    @Override
    public String getMappingPath() {
        return MAPPING;
    }

    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT;
    }

    @Override
    public boolean insert(ComponentInspection insp) throws DaoException {
        ensureExists(insp, "Component inspection cannot be null.");
        ensureExists(insp.getId(), "Component inspection ID is required.");
        ComponentInspection pi = retrieve(insp.getId());
        if (pi == null) {
            indexObject(insp.getId(), insp);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(ComponentInspection insp) throws DaoException {
        ensureExists(insp, "Component inspection cannot be null.");
        ensureExists(insp.getId(), "Component inspection ID is required.");
        ComponentInspection pi = retrieve(insp.getId());
        if (pi == null) {
            return false;
        } else {
            indexObject(insp.getId(), insp);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Component inspection ID is required.");
        deleteDocument(id);
        return true;
    }

    @Override
    public ComponentInspection retrieve(String id) throws DaoException {
        ensureExists(id, "Component inspection ID is required.");
        return retrieveObject(id, ComponentInspection.class);
    }

    @Override
    public List<ComponentInspection> search(ComponentInspectionSearchParams params) throws DaoException {
        ensureExists(params, "Search parameters are required.");
        if (!params.hasCriteria()) {
            throw new DaoException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_SITE_INSP_ID, params.getSiteInspectionId());
        query = addQueryMust(query, COL_ASSET_ID, params.getAssetId());
        query = addQueryMust(query, COL_ASSET_INSPECTION_ID, params.getAssetInspectionId());
        query = addQueryMust(query, COL_COMP_ID, params.getComponentId());
        query = addQueryMust(query, COL_ORDER_NUM, params.getOrderNumber());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
        query = addQueryMust(query, COL_STATUS, params.getStatus());
        query = addQueryMust(query, COL_TYPE, params.getType());
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(ComponentInspection.class, response, scrollLifeLimit);
    }
}

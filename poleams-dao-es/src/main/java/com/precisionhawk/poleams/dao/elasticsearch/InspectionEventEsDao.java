package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.dao.InspectionEventDao;
import com.precisionhawk.poleams.domain.InspectionEvent;
import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import java.util.List;
import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

/**
 *
 * @author pchapman
 */
@Named
public class InspectionEventEsDao extends AbstractEsDao implements InspectionEventDao {
    
    protected static final String DOCUMENT_TYPE = "InspectionEvent";
    protected static final String FIELD_ASSET_ID = "assetId";
    protected static final String FIELD_COMPONENT = "componentId";
    protected static final String FIELD_ORDER_NUMBER = "orderNumber";
    protected static final String FIELD_SITE_ID = "siteId";
    protected static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/InspectionEvent_Mapping.json";

    @Override
    protected String getIndexName() {
        return ElasticSearchConstants.INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT_TYPE;
    }

    @Override
    protected String getMappingPath() {
        return MAPPING;
    }
    
    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Inspection event ID is required.");
        super.deleteDocument(id);
        return true;
    }

    @Override
    public InspectionEvent retrieve(String id) throws DaoException {
        ensureExists(id, "Inspection event ID is required.");
        return super.retrieveObject(id, InspectionEvent.class);
    }

    private QueryBuilder buildLookupQuery(String siteId, String orderNumber, String assetId, String componentId) throws DaoException {
        BoolQueryBuilder query = addQueryMust(null, FIELD_SITE_ID, siteId);
        query = addQueryMust(query, FIELD_ORDER_NUMBER, orderNumber);
        query = addQueryMust(query, FIELD_ASSET_ID, assetId);
        query = addQueryMust(query, FIELD_COMPONENT, componentId);
        return query;
   }

    @Override
    public Long count(InspectionEventSearchParams searchParms) throws DaoException {
        ensureExists(searchParms, "Search parameters are required.");
        QueryBuilder queryBuilder = buildLookupQuery(searchParms.getSiteId(), searchParms.getOrderNumber(), searchParms.getAssetId(), searchParms.getComponentId());
        ensureExists(queryBuilder, "Search parameters are required.");
        
        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setTypes(DOCUMENT_TYPE)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setQuery(queryBuilder)
                        .setSize(0);
        LOGGER.debug("Executing the following query: {}", search.toString());
        SearchResponse response = search.execute().actionGet();

        return response.getHits().totalHits();
    }

    @Override
    public List<InspectionEvent> search(InspectionEventSearchParams searchParms) throws DaoException {
        ensureExists(searchParms, "Search parameters are required.");
        QueryBuilder queryBuilder = buildLookupQuery(searchParms.getSiteId(), searchParms.getOrderNumber(), searchParms.getAssetId(), searchParms.getComponentId());
        ensureExists(queryBuilder, "Search parameters are required.");
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());

        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setTypes(DOCUMENT_TYPE)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setQuery(queryBuilder)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());
        LOGGER.debug("Executing the following query: {}", search.toString());
        SearchResponse response = search.execute().actionGet();

        return loadFromScrolledSearch(InspectionEvent.class, response, scrollLifeLimit);
    }

    @Override
    public boolean insert(InspectionEvent event) throws DaoException {
        ensureExists(event, "Inspection event is required.");
        ensureExists(event.getId(), "Inspection event ID is required.");
        InspectionEvent e = retrieve(event.getId());
        if (e == null) {
            indexObject(event.getId(), event);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(InspectionEvent event) throws DaoException {
        ensureExists(event, "Inspection event is required.");
        ensureExists(event.getId(), "Inspection event ID is required.");
        InspectionEvent e = retrieve(event.getId());
        if (e == null) {
            return false;
        } else {
            indexObject(event.getId(), event);
            return true;
        }
    }
}

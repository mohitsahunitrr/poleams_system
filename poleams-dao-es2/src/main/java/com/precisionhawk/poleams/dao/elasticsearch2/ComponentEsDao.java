package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.ams.bean.ComponentSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch2.AbstractEsDao;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.poleams.dao.ComponentDao;
import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import java.util.List;
import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;

/**
 *
 * @author pchapman
 */
@Named
public class ComponentEsDao extends AbstractEsDao implements ComponentDao, ElasticSearchConstants {
    
    private static final String COL_ASSET_ID = "assetId";
    private static final String COL_SERIAL_NUM = "serialNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_TYPE = "type";
    private static final String DOCUMENT = "Component";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch2/Component_Mapping.json";

    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT;
    }

    @Override
    protected String getMappingPath() {
        return MAPPING;
    }

    @Override
    public boolean insert(Component comp) throws DaoException {
        ensureExists(comp, "Component is required");
        ensureExists(comp.getId(), "Component ID is required");
        Component c = retrieve(comp.getId());
        if (c == null) {
            indexObject(comp.getId(), comp);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(Component comp) throws DaoException {
        ensureExists(comp, "Component is required");
        ensureExists(comp.getId(), "Component ID is required");
        Component c = retrieve(comp.getId());
        if (c == null) {
            return false;
        } else {
            indexObject(comp.getId(), comp);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Component ID is required");
        deleteDocument(id);
        return true;
    }

    @Override
    public Component retrieve(String id) throws DaoException {
        ensureExists(id, "Component ID is required");
        return retrieveObject(id, Component.class);
    }

    @Override
    public List<Component> search(ComponentSearchParams params) throws DaoException {
        ensureExists(params, "Search parameters are required");
        if (!params.hasCriteria()) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = null;
        query = addQueryMust(query, COL_ASSET_ID, params.getAssetId());
        query = addQueryMust(query, COL_SERIAL_NUM, params.getSerialNumber());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
        query = addQueryMust(query, COL_TYPE, params.getType());
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(INDEX_NAME_POLEAMS)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(Component.class, response, scrollLifeLimit);
    }
    
}

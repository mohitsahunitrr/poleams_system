package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.dao.FeederInspectionDao;
import com.precisionhawk.poleams.domain.FeederInspection;
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
public class FeederInspectionEsDao extends AbstractEsDao implements FeederInspectionDao {

    private static final String COL_ORDER_NUM = "orderNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_STATUS = "status";
    private static final String COL_TYPE = "type";
    private static final String DOCUMENT = "SiteInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/FeederInspection_Mapping.json";

    @Override
    protected String getIndexName() {
        return ElasticSearchConstants.INDEX_NAME_POLEAMS;
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
    public boolean insert(FeederInspection inspection) throws DaoException {
        ensureExists(inspection, "Feeder inspection required.");
        ensureExists(inspection.getId(), "Unique ID required for asset inspection.");
        FeederInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            super.indexObject(inspection.getId(), inspection);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(FeederInspection inspection) throws DaoException {
        ensureExists(inspection, "Feeder inspection required.");
        ensureExists(inspection.getId(), "Unique ID required for asset inspection.");
        FeederInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            return false;
        } else {
            super.indexObject(inspection.getId(), inspection);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Unique ID required for asset inspection.");
        super.deleteDocument(id);
        return true;
    }

    @Override
    public FeederInspection retrieve(String id) throws DaoException {
        ensureExists(id, "Unique ID required for asset inspection.");
        return super.retrieveObject(id, FeederInspection.class);
    }

    @Override
    public List<FeederInspection> search(SiteInspectionSearchParams params) throws DaoException {
        ensureExists(params, "Search parameters are required.");
        if (!params.hasCriteria()) {
            throw new DaoException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_ORDER_NUM, params.getOrderNumber());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
        if (params.getStatus() != null) {
            query = addQueryMust(query, COL_STATUS, params.getStatus().getValue());
        }
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
        return loadFromScrolledSearch(FeederInspection.class, response, scrollLifeLimit);
    }    
}

package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch2.AbstractEsDao;
import com.precisionhawk.poleams.dao.TransmissionLineInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
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
public class TransmissionLineInspectionEsDao extends AbstractEsDao implements TransmissionLineInspectionDao {

    private static final String COL_ORDER_NUM = "orderNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_STATUS = "status";
    private static final String COL_TYPE = "type";
    private static final String DOCUMENT = "TransmissionLineInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch2/TransmissionLineInspection_Mapping.json";

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
    public boolean insert(TransmissionLineInspection inspection) throws DaoException {
        ensureExists(inspection, "Transmission Line inspection required.");
        ensureExists(inspection.getId(), "Unique ID required for transmission line inspection.");
        TransmissionLineInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            super.indexObject(inspection.getId(), inspection);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionLineInspection inspection) throws DaoException {
        ensureExists(inspection, "Transmission Line inspection required.");
        ensureExists(inspection.getId(), "Unique ID required for transmission line inspection.");
        TransmissionLineInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            return false;
        } else {
            super.indexObject(inspection.getId(), inspection);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Unique ID required for transmission line inspection.");
        super.deleteDocument(id);
        return true;
    }

    @Override
    public TransmissionLineInspection retrieve(String id) throws DaoException {
        ensureExists(id, "Unique ID required for transmission line inspection.");
        return super.retrieveObject(id, TransmissionLineInspection.class);
    }

    @Override
    public List<TransmissionLineInspection> search(SiteInspectionSearchParams params) throws DaoException {
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
        return loadFromScrolledSearch(TransmissionLineInspection.class, response, scrollLifeLimit);
    }    
}

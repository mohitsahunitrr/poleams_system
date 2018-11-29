package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.dao.TransmissionStructureInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
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
public class TransmissionStructureInspectionEsDao extends AbstractEsDao implements TransmissionStructureInspectionDao, ElasticSearchConstants {
    
    private static final String COL_ID = "id";
    private static final String COL_ASSET_ID = "assetId";
    private static final String COL_ORDER_NUM = "orderNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_SITE_INSP_ID = "siteInspectionId";
    private static final String COL_STATUS = "status";
    private static final String DOCUMENT = "TransmissionStructureInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/TransmissionStructureInspection_Mapping.json";

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
    public boolean insert(TransmissionStructureInspection inspection) throws DaoException {
        ensureExists(inspection, "Transmission Structure inspection cannot be null.");
        ensureExists(inspection.getId(), "Transmission Structure inspection ID is required.");
        TransmissionStructureInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            indexObject(inspection.getId(), inspection);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionStructureInspection inspection) throws DaoException {
        ensureExists(inspection, "Transmission Structure inspection cannot be null.");
        ensureExists(inspection.getId(), "Transmission Structure inspection ID is required.");
        TransmissionStructureInspection pi = retrieve(inspection.getId());
        if (pi == null) {
            return false;
        } else {
            indexObject(inspection.getId(), inspection);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Transmission Structure inspection ID is required.");
        deleteDocument(id);
        return true;
    }

    @Override
    public TransmissionStructureInspection retrieve(String id) throws DaoException {
        ensureExists(id, "Transmission Structure inspection ID is required.");
        return retrieveObject(id, TransmissionStructureInspection.class);
    }

    @Override
    public List<TransmissionStructureInspection> search(AssetInspectionSearchParams params) throws DaoException {
        ensureExists(params, "Search parameters are required.");
        if (!params.hasCriteria()) {
            throw new DaoException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_SITE_INSP_ID, params.getSiteInspectionId());
        query = addQueryMust(query, COL_ASSET_ID, params.getAssetId());
        query = addQueryMust(query, COL_ORDER_NUM, params.getOrderNumber());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
        query = addQueryMust(query, COL_STATUS, params.getStatus());
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(TransmissionStructureInspection.class, response, scrollLifeLimit);
    }
}

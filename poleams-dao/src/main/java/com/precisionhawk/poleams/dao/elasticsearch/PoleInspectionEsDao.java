package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.dao.PoleInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
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
public class PoleInspectionEsDao extends AbstractEsDao implements PoleInspectionDao, ElasticSearchConstants {
    
    private static final String COL_ID = "id";
    private static final String COL_ASSET_ID = "assetId";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_SITE_INSP_ID = "siteInspectionId";
    private static final String DOCUMENT = "PoleInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/PoleInspection_Mapping.json";

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
    public boolean insert(PoleInspection poleInspection) throws DaoException {
        if (poleInspection == null) {
            throw new IllegalArgumentException("Pole inspection cannot be null.");
        } else if (poleInspection.getId() == null || poleInspection.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole inspection ID is required.");
        }
        PoleInspection pi = retrieve(poleInspection.getId());
        if (pi == null) {
            indexObject(poleInspection.getId(), poleInspection);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(PoleInspection poleInspection) throws DaoException {
        if (poleInspection == null) {
            throw new IllegalArgumentException("Pole inspection cannot be null.");
        } else if (poleInspection.getId() == null || poleInspection.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole inspection ID is required.");
        }
        PoleInspection pi = retrieve(poleInspection.getId());
        if (pi == null) {
            return false;
        } else {
            indexObject(poleInspection.getId(), poleInspection);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Pole inspection ID is required.");
        }
        deleteDocument(id);
        return true;
    }

    @Override
    public PoleInspection retrieve(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Pole inspection ID is required.");
        }
        return retrieveObject(id, PoleInspection.class);
    }

    @Override
    public List<PoleInspection> search(AssetInspectionSearchParams params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_SITE_INSP_ID, params.getSiteInspectionId());
        query = addQueryMust(query, COL_ASSET_ID, params.getAssetId());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(INDEX_NAME_POLEAMS)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(PoleInspection.class, response, scrollLifeLimit);
    }
}

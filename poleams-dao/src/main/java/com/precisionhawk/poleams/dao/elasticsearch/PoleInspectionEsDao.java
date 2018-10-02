package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import us.pcsw.es.util.ESUtils;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class PoleInspectionEsDao extends AbstractEsDao implements PoleInspectionDao {
    
    private static final String COL_ID = "id";
    private static final String COL_ORG_ID = "organizationId";
    private static final String COL_POLE_ID = "poleId";
    private static final String COL_SS_ID = "subStationId";
    private static final String DOCUMENT = "PoleInspection";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/PoleInspection_Mapping.json";

    @Override
    public String getMappingPath() {
        return MAPPING;
    }

    @PostConstruct
    public void init() {
        try {
            ESUtils.ensureMapping(getClient(), INDEX_NAME_POLEAMS, DOCUMENT, MAPPING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
    public List<PoleInspection> search(PoleInspectionSearchParameters params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_ORG_ID, params.getOrganizationId());
        query = addQueryMust(query, COL_POLE_ID, params.getPoleId());
        query = addQueryMust(query, COL_SS_ID, params.getSubStationId());
        
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

package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.SubStationDao;
import com.precisionhawk.poleams.domain.SubStation;
import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;

/**
 *
 * @author pchapman
 */
public class SubStationEsDao extends AbstractEsDao implements SubStationDao {
    
    private static final String DOCUMENT = "Pole";
    private static final String COL_FEEDER_NUM = "feederNumber";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_ORG_ID = "organizationId";

    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT;
    }

    @Override
    public boolean insert(SubStation substation) throws DaoException {
        if (substation == null) {
            throw new IllegalArgumentException("SubStation cannot be null.");
        } else if (substation.getId() == null || substation.getId().isEmpty()) {
            throw new IllegalArgumentException("SubStation ID is required.");
        }
        SubStation ss = retrieve(substation.getId());
        if (ss == null) {
            indexObject(ss.getId(), substation);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(SubStation substation) throws DaoException {
        if (substation == null) {
            throw new IllegalArgumentException("SubStation cannot be null.");
        } else if (substation.getId() == null || substation.getId().isEmpty()) {
            throw new IllegalArgumentException("SubStation ID is required.");
        }
        SubStation ss = retrieve(substation.getId());
        if (ss == null) {
            return false;
        } else {
            indexObject(substation.getId(), substation);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("SubStation ID is required.");
        }
        deleteDocument(id);
        return true;
    }

    @Override
    public SubStation retrieve(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("SubStation ID is required.");
        }
        return retrieveObject(id, SubStation.class);
    }

    @Override
    public List<SubStation> search(SubStationSearchParameters params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_FEEDER_NUM, params.getFeederNumber());
        query = addQueryMust(query, COL_NAME, params.getName());
        query = addQueryMust(query, COL_ORG_ID, params.getOrganizationId());
        if (query == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(INDEX_NAME_POLEAMS)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(SubStation.class, response, scrollLifeLimit);
    }
}

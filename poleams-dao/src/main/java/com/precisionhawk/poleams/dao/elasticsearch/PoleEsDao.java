package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.data.PoleData;
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
public class PoleEsDao extends AbstractEsDao implements PoleDao {
    
    private static final String DOCUMENT = "Pole";
    private static final String COL_ID = "id";
    private static final String COL_FPL_ID = "fplid";
    private static final String COL_ORG_ID = "organizationId";
    private static final String COL_SS_ID = "subStationId";

    @Override
    public boolean insert(PoleData pole) throws DaoException {
        if (pole == null) {
            throw new IllegalArgumentException("Pole cannot be null.");
        } else if (pole.getId() == null || pole.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        PoleData p = retrieve(pole.getId());
        if (p == null) {
            indexObject(pole.getId(), pole);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(PoleData pole) throws DaoException {
        if (pole == null) {
            throw new IllegalArgumentException("Pole cannot be null.");
        } else if (pole.getId() == null || pole.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        PoleData p = retrieve(pole.getId());
        if (p == null) {
            return false;
        } else {
            indexObject(pole.getId(), pole);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        deleteDocument(id);
        return true;
    }

    @Override
    public PoleData retrieve(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        return retrieveObject(id, PoleData.class);
    }

    @Override
    public List<PoleData> search(PoleSearchParameters params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_FPL_ID, params.getFPLId());
        query = addQueryMust(query, COL_ORG_ID, params.getOrganizationId());
        query = addQueryMust(query, COL_SS_ID, params.getSubStationId());
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
        return loadFromScrolledSearch(PoleData.class, response, scrollLifeLimit);
    }

    @Override
    protected String getIndexName() {
        return INDEX_NAME_POLEAMS;
    }

    @Override
    protected String getDocumentType() {
        return DOCUMENT;
    }
}

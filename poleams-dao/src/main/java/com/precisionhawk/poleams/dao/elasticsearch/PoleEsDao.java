package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.domain.Pole;
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
public class PoleEsDao extends AbstractEsDao implements PoleDao, ElasticSearchConstants {
    
    private static final String COL_ID = "id";
    private static final String COL_UTILITY_ID = "utilityId";
    private static final String COL_ORG_ID = "organizationId";
    private static final String COL_SITE_ID = "siteId";
    private static final String DOCUMENT = "Asset";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/Pole_Mapping.json";

    @Override
    public String getMappingPath() {
        return MAPPING;
    }

    @Override
    public boolean insert(Pole pole) throws DaoException {
        if (pole == null) {
            throw new IllegalArgumentException("Pole cannot be null.");
        } else if (pole.getId() == null || pole.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        Pole p = retrieve(pole.getId());
        if (p == null) {
            indexObject(pole.getId(), pole);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(Pole pole) throws DaoException {
        if (pole == null) {
            throw new IllegalArgumentException("Pole cannot be null.");
        } else if (pole.getId() == null || pole.getId().isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        Pole p = retrieve(pole.getId());
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
    public Pole retrieve(String id) throws DaoException {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Pole ID is required.");
        }
        return retrieveObject(id, Pole.class);
    }

    @Override
    public List<Pole> search(PoleSearchParams params) throws DaoException {
        if (params == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_UTILITY_ID, params.getUtilityId());
        query = addQueryMust(query, COL_ORG_ID, params.getOrganizationId());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
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
        return loadFromScrolledSearch(Pole.class, response, scrollLifeLimit);
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

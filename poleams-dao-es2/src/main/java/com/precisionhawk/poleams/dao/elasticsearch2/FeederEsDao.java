package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.ams.bean.SiteSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.SiteProvider;
import com.precisionhawk.ams.dao.elasticsearch2.AbstractEsDao;
import com.precisionhawk.ams.domain.Site;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import static com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants.INDEX_NAME_POLEAMS;
import java.util.List;
import javax.inject.Named;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import com.precisionhawk.poleams.dao.FeederDao;
import java.util.LinkedList;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class FeederEsDao extends AbstractEsDao implements FeederDao, ElasticSearchConstants, SiteProvider {
    
    private static final String COL_FEEDER_NUM = "feederNumber";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_ORG_ID = "organizationId";
    private static final String DOCUMENT = "Feeder";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch2/Feeder_Mapping.json";

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
    public boolean insert(Feeder substation) throws DaoException {
        ensureExists(substation, "Feeder is required.");
        ensureExists(substation.getId(), "Feeder ID is required.");
        Feeder ss = retrieve(substation.getId());
        if (ss == null) {
            indexObject(substation.getId(), substation);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(Feeder substation) throws DaoException {
        ensureExists(substation, "Feeder is required.");
        ensureExists(substation.getId(), "Feeder ID is required.");
        Feeder ss = retrieve(substation.getId());
        if (ss == null) {
            return false;
        } else {
            indexObject(substation.getId(), substation);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Feeder ID is required.");
        deleteDocument(id);
        return true;
    }

    @Override
    public Feeder retrieve(String id) throws DaoException {
        ensureExists(id, "Feeder ID is required.");
        return retrieveObject(id, Feeder.class);
    }

    @Override
    public List<Feeder> search(FeederSearchParams params) throws DaoException {
        if (params == null || !params.hasCriteria()) {
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
        return loadFromScrolledSearch(Feeder.class, response, scrollLifeLimit);
    }

    @Override
    public List<Feeder> retrieveAll() throws DaoException {
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(INDEX_NAME_POLEAMS)
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();

        return loadFromScrolledSearch(Feeder.class, response, scrollLifeLimit);
    }

    @Override
    public List<Site> retrieve(SiteSearchParams params) throws DaoException {
        return (List<Site>)((List<? extends Site>)search(new FeederSearchParams(params)));
    }
    
    @Override
    public List<Site> retrieveAllSites() throws DaoException {
        return (List<Site>)((List<? extends Site>)retrieveAll());
    }

    @Override
    public List<Site> retrieveByIDs(List<String> siteIDs) throws DaoException {
        List<Site> list = new LinkedList<>();
        for (String id : siteIDs) {
            list.add(retrieve(id));
        }
        return list;
    }
}

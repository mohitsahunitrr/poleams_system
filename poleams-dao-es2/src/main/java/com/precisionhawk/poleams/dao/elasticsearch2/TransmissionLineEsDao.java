package com.precisionhawk.poleams.dao.elasticsearch2;

import com.precisionhawk.ams.bean.SiteSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.SiteProvider;
import com.precisionhawk.ams.dao.elasticsearch2.AbstractEsDao;
import com.precisionhawk.ams.domain.Site;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.domain.TransmissionLine;
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
import com.precisionhawk.poleams.dao.TransmissionLineDao;
import java.util.LinkedList;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class TransmissionLineEsDao extends AbstractEsDao implements TransmissionLineDao, ElasticSearchConstants, SiteProvider {
    
    private static final String COL_LINE_NUM = "lineNumber";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_ORG_ID = "organizationId";
    private static final String DOCUMENT = "TransmissionLine";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch2/TransmissionLine_Mapping.json";

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
    public boolean insert(TransmissionLine line) throws DaoException {
        ensureExists(line, "Transmission Line is required.");
        ensureExists(line.getId(), "Transmission Line ID is required.");
        TransmissionLine ss = retrieve(line.getId());
        if (ss == null) {
            indexObject(line.getId(), line);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionLine line) throws DaoException {
        ensureExists(line, "Transmission Line is required.");
        ensureExists(line.getId(), "Transmission Line ID is required.");
        TransmissionLine ss = retrieve(line.getId());
        if (ss == null) {
            return false;
        } else {
            indexObject(line.getId(), line);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "Transmission Line ID is required.");
        deleteDocument(id);
        return true;
    }

    @Override
    public TransmissionLine retrieve(String id) throws DaoException {
        ensureExists(id, "Transmission Line ID is required.");
        return retrieveObject(id, TransmissionLine.class);
    }

    @Override
    public List<TransmissionLine> search(TransmissionLineSearchParams params) throws DaoException {
        if (params == null || !params.hasCriteria()) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_LINE_NUM, params.getLineNumber());
        query = addQueryMust(query, COL_NAME, params.getName());
        query = addQueryMust(query, COL_ORG_ID, params.getOrganizationId());
        if (query == null) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(query)
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();
        return loadFromScrolledSearch(TransmissionLine.class, response, scrollLifeLimit);
    }

    @Override
    public List<TransmissionLine> retrieveAll() throws DaoException {
        TimeValue scrollLifeLimit = new TimeValue(getScrollLifespan());
        SearchRequestBuilder search =
                getClient().prepareSearch(getIndexName())
                        .setSearchType(SearchType.QUERY_AND_FETCH)
                        .setTypes(DOCUMENT)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .setScroll(scrollLifeLimit)
                        .setSize(getScrollSize());

        SearchResponse response = search.execute().actionGet();

        return loadFromScrolledSearch(TransmissionLine.class, response, scrollLifeLimit);
    }

    @Override
    public List<Site> retrieve(SiteSearchParams params) throws DaoException {
        return (List<Site>)((List<? extends Site>)search(new TransmissionLineSearchParams(params)));
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

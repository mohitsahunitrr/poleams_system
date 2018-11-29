package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.dao.elasticsearch.AbstractEsDao;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.dao.TransmissionStructureDao;
import com.precisionhawk.poleams.domain.TransmissionStructure;
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
public class TransmissionStructureEsDao extends AbstractEsDao implements TransmissionStructureDao, ElasticSearchConstants {
    
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_SERIAL_NUM = "serialNumber";
    private static final String COL_STRUCTURE_NUM = "structureNumber";
    private static final String COL_SITE_ID = "siteId";
    private static final String COL_TYPE = "type";
    private static final String DOCUMENT = "TransmissionStructure";
    private static final String MAPPING = "com/precisionhawk/poleams/dao/elasticsearch/TransmissionStructure_Mapping.json";

    @Override
    public String getMappingPath() {
        return MAPPING;
    }

    @Override
    public boolean insert(TransmissionStructure structure) throws DaoException {
        ensureExists(structure, "TransmissionStructure is required");
        ensureExists(structure.getId(), "TransmissionStructure ID is required");
        TransmissionStructure p = retrieve(structure.getId());
        if (p == null) {
            indexObject(structure.getId(), structure);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean update(TransmissionStructure structure) throws DaoException {
        ensureExists(structure, "TransmissionStructure is required");
        ensureExists(structure.getId(), "TransmissionStructure ID is required");
        TransmissionStructure p = retrieve(structure.getId());
        if (p == null) {
            return false;
        } else {
            indexObject(structure.getId(), structure);
            return true;
        }
    }

    @Override
    public boolean delete(String id) throws DaoException {
        ensureExists(id, "TransmissionStructure ID is required");
        deleteDocument(id);
        return true;
    }

    @Override
    public TransmissionStructure retrieve(String id) throws DaoException {
        ensureExists(id, "TransmissionStructure ID is required");
        return retrieveObject(id, TransmissionStructure.class);
    }

    @Override
    public List<TransmissionStructure> search(TransmissionStructureSearchParams params) throws DaoException {
        ensureExists(params, "Search parameters are required");
        if (!params.hasCriteria()) {
            throw new IllegalArgumentException("Search parameters are required.");
        }
        BoolQueryBuilder query = addQueryMust(null, COL_STRUCTURE_NUM, params.getStructureNumber());
        query = addQueryMust(query, COL_NAME, params.getName());
        query = addQueryMust(query, COL_SERIAL_NUM, params.getSerialNumber());
        query = addQueryMust(query, COL_SITE_ID, params.getSiteId());
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
        return loadFromScrolledSearch(TransmissionStructure.class, response, scrollLifeLimit);
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

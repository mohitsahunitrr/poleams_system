package com.precisionhawk.poleams.dao.elasticsearch;

import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConfig;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.support.elasticsearch.ElasticSearchConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.papernapkin.liana.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.pcsw.es.util.ESUtils;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class AbstractEsDao implements ElasticSearchConstants {

    @Inject protected Client client;
    @Inject protected ElasticSearchConfig config;
    @Inject protected ObjectMapper mapper;
    protected final Logger LOGGER;
    
    protected AbstractEsDao() {
        LOGGER = LoggerFactory.getLogger(getClass());
    }

    protected Integer getBulkSize() {
        return config.getBulkSize();
    }
    
    protected Client getClient() {
        return client;
    }
    
    public long getScrollLifespan() {
        return config.getScrollLifespan();
    }
    
    public int getScrollSize() {
        return config.getScrollSize();
    }

    @PostConstruct
    public void init() {
        try {
            ESUtils.ensureMapping(getClient(), getIndexName(), getDocumentType(), getMappingPath());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Serialize an object to string.
     *
     * @param object the object
     * @return the String representation
     * @throws DaoException on error
     */
    public String serializeToString(Object object) throws DaoException {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            LOGGER.error("Unable to map object {}", object);
            throw new DaoException("Unable to map the object " + object, e);
        }
    }

    /**
     * Deserializes a JSON string to the specified object type
     *
     * @param json the string to deserialize
     * @param klass the object type to use
     * @return the object
     * @throws DaoException on error
     */
    public <T> T deserializeToObject(String json, Class<T> klass) throws DaoException {
        try {
            return mapper.readValue(json, klass);
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize object from JSON", e);
            LOGGER.error("{}", json);
            throw new DaoException("Unable to deserialize object", e);
        }
    }

    /**
     * Indexes a single object into elastic search
     *
     * @param object the object to index
     * @throws DaoException on error
     */
    protected void indexObject(String id, Object object)
            throws DaoException
    {
        if (object == null) {
            return;
        }

        String json = serializeToString(object);
        restore(id, json);
    }
    
    public boolean restore(String id, String json) {
    	id = StringUtil.nullableNotEmptyTrimmed(id);
    	json = StringUtil.nullableNotEmptyTrimmed(json);
    	if (id == null || json == null) {
    		return false;
    	} else {
            try {
                getClient().prepareIndex(getIndexName(), getDocumentType(), id)
                        .setSource(json)
                        .setRefresh(true)
                        .execute()
                        .actionGet();
                return true;
            } catch (Throwable t) {
                LOGGER.error("Error indexing {}:: {}", getDocumentType(), json);
                LOGGER.error("Error indexing {}:: {}", getDocumentType(), t);
                throw t;
            }
    	}
    }
    
    protected <T> T retrieveObject(String id, Class<T> clazz)
            throws DaoException
    {
        try {
            GetResponse response = getClient().prepareGet(getIndexName(), getDocumentType(), id)
                    .execute()
                    .actionGet();
            return getObjectFromResponse(clazz, response);
        } catch (DaoException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new DaoException(String.format("Error retrieving object of class %s with ID %s", clazz.toString(), id), t);
        }
    }
    
    protected <T> List<T> retrieveObjects(List<String> ids, Class<T> clazz)
            throws DaoException
    {
        MultiGetRequestBuilder request = getClient().prepareMultiGet();
        for (String id : ids) {
            request.add(getIndexName(), getDocumentType(), id);
        }
        MultiGetResponse response = request.execute().actionGet();
        return getObjectsFromResponse(clazz, response);
    }
    
    protected <T> List<T> loadFromScrolledSearch(Class<T> clazz, SearchResponse resp, TimeValue scrollLifeLimit)
            throws DaoException
    {
        List<T> list = new LinkedList<>();
        for (boolean hitsRead = true ; hitsRead ; resp = getClient().prepareSearchScroll(resp.getScrollId()).setScroll(scrollLifeLimit).execute().actionGet())
        {
            hitsRead = false;
            for (SearchHit hit : resp.getHits()) {
                hitsRead = true;
                list.add(deserializeToObject(hit.sourceAsString(), clazz));
            }
        }
        return list;
    }

    /**
     * Deletes a single document from an elastic search index
     *
     * @param id the id of the document to delete
     * @throws DaoException on error
     */
    public void deleteDocument(String id) {
        if (id == null) {
            return;
        }

        getClient().prepareDelete(getIndexName(), getDocumentType(), id)
                .execute()
                .actionGet();
    }

    /**
     * Deletes a list of documents from an elastic search index
     *
     * @param ids the ids of the documents to delete
     */
    public void deleteObjects(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (int i = 0; i <= ids.size(); i += getBulkSize()) {
            BulkRequestBuilder bulk = client.prepareBulk();

            Integer toIndex = i + getBulkSize();
            if (toIndex > ids.size()) {
                toIndex = ids.size();
            }
            List<String> subObjects = ids.subList(i, toIndex);
            for (String id : subObjects) {
                bulk.add(client.prepareDelete(getIndexName(), getDocumentType(), id));
            }

            bulk.execute().actionGet();
        }
    }
    
    /**
     * Marshalls a {@link org.elasticsearch.action.get.MultiGetResponse} of objects into their java objects
     *
     * @param clazz the class of the object to marshal
     * @param response the get response
     * @return the collection
     * @throws DaoException on error
     */
    protected <T> List<T> getObjectsFromResponse(Class<T> clazz, MultiGetResponse response)
            throws DaoException
    {
        List<T> results = new ArrayList<T>(response.getResponses().length);

        for (MultiGetItemResponse item : response.getResponses()) {
            if (item.getResponse().isExists()) {
                T object = deserializeToObject(item.getResponse().getSourceAsString(), clazz);
                results.add(object);
            }
        }
        return results;
    }
    
    /**
     * Marshalls a {@link org.elasticsearch.action.get.MultiGetResponse} of objects into their java objects
     *
     * @param clazz the class of the object to marshal
     * @param response the get response
     * @return the collection
     * @throws DaoException on error
     */
    protected <T> List<T> getObjectsFromResponse(Class<T> clazz, SearchResponse response)
            throws DaoException
    {
        if (response.getHits().getTotalHits() > Integer.MAX_VALUE) {
            throw new RuntimeException("Too many hits");
        }
        List<T> results = new ArrayList<T>((int)response.getHits().getTotalHits());
        for (SearchHit hit : response.getHits()) {
            T object = deserializeToObject(hit.getSourceAsString(), clazz);
            results.add(object);
        }
        return results;
    }

    /**
     * Marshals a single {@link GetResponse}s JSON source (if it exists) into an object
     *
     * @param clazz the class of the object to marshal
     * @param response the response from the request
     * @return the marshaled java object
     * @throws DaoException on error
     */
    protected <T> T getObjectFromResponse(Class<T> clazz, GetResponse response)
            throws DaoException
    {
        if (!response.isExists() || response.isSourceEmpty()) {
            return null;
        }

        return deserializeToObject(response.getSourceAsString(), clazz);
    }

    /**
     * Adds the field and value to the query as a must.
     * @param queryBuilder
     * @param fieldName
     * @param value 
     */
    protected void addFieldToQuery(BoolQueryBuilder queryBuilder, String fieldName, String value) {
        value = StringUtil.nullableNotEmptyTrimmed(value);
        if (value != null) {
            queryBuilder.must(QueryBuilders.termQuery(fieldName, value));
        }
    }


    /**
     * Indexes a single object into elastic search using raw document data.  This method is not published
     * in the API as it is dangerous and is intended for restoring backup data.
     * 
     * <strong>No attempt to ensure the data is correct beyond ElasticSearch's mapping checks.</strong>
     *
     * @param id The unique ID of the document.
     * @param source The data to be indexed, in JSON.
     * @throws DaoException on error
     */
    public void indexObject(String id, String source)
            throws DaoException
    {
        if (id != null || source == null) {
            return;
        }

        try {
            getClient().prepareIndex(getIndexName(), getDocumentType(), id)
                    .setSource(source)
                    .setRefresh(true)
                    .execute()
                    .actionGet();
        } catch (Throwable t) {
            LOGGER.error("Error indexing document:: {}", source);
            LOGGER.error("Error indexing document:: ", t);
            throw t;
        }
    }
    
    protected abstract String getIndexName();
    
    protected abstract String getDocumentType();
    
    protected abstract String getMappingPath();
    
    protected BoolQueryBuilder addQueryMust(BoolQueryBuilder original, QueryBuilder toAdd) {
        if (original == null) {
            return QueryBuilders.boolQuery().must(toAdd);
        } else {
            return original.must(toAdd);
        }
    }
    
    /**
     * Adds a must for a term query for the given field IF the value is not null and not empty string.
     * @param original The original query to which the MUST should be appended.  May be null for a new query.
     * @param fieldName The name of the field to be searched.
     * @param value The value to be searched for.  If null, no MUST will be appended or new query generated.
     * @return The new query builder generated from the operation.
     */
    protected BoolQueryBuilder addQueryMust(BoolQueryBuilder original, String fieldName, Object value) {
        QueryBuilder queryBuilder = null;
        value = StringUtil.nullableNotEmptyTrimmed(StringUtil.nullableToString(value));
        if (value != null) {
            queryBuilder = QueryBuilders.termQuery(fieldName, value);
            if (original == null) {
                return QueryBuilders.boolQuery().must(queryBuilder);
            } else {
                return original.must(queryBuilder);
            }
        } else {
            return original;
        }
    }
}

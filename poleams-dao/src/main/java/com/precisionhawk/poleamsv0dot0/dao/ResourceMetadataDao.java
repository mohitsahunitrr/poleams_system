package com.precisionhawk.poleamsv0dot0.dao;

import com.precisionhawk.poleamsv0dot0.bean.ResourceSearchParameters;
import com.precisionhawk.poleamsv0dot0.domain.ResourceMetadata;
import java.util.List;

/**
 * Storage for resource metadata.
 *
 * @author Philip A. Chapman
 */
public interface ResourceMetadataDao {
    
    /**
     * Retrieves the metadata associated with a resource by the resource's
     * unique key.
     * 
     * @param resourceId The unique ID of the resource.
     * @return The resource's metadata.
     * @throws DaoException Indicates an irrecoverable error.
     */
    ResourceMetadata retrieveResourceMetadata(String resourceId) throws DaoException;
    
    /**
     * Searches for Resource Metadata that fits all given criteria.  At least one parameter must be non-null.
     * @param params The bean holding filter criteria for the request.
     * @return A list of all matching resource metadata.
     * @throws DaoException Indicates an irrecoverable error.
     */
    List<ResourceMetadata> lookup(ResourceSearchParameters params) throws DaoException;
    
    boolean insertMetadata(ResourceMetadata meta) throws DaoException;

    boolean deleteMetadata(String id) throws DaoException;
    
    boolean updateMetadata(ResourceMetadata meta) throws DaoException;
}

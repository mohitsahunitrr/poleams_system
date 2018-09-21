package com.precisionhawk.poleams.repository;

import com.precisionhawk.poleams.domain.ResourceMetadata;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pchapman
 */
public interface ResourceRepository {

    /**
     * Stores the resource into the repository using the unique key.
     * 
     * @param metadata Metadata about the resource.
     * @param key Key for the data.
     * @throws RepositoryException Indicates an irrecoverable error.
     */
    void storeResource(ResourceMetadata metadata, String key, String name, String contentType, InputStream resourceStream, Long length) throws RepositoryException;
    
    /**
     * Retrieves the resource through a stream.
     * 
     * @param key The unique key of the resource.
     * @return An input stream through which the resource may be loaded.
     * @throws RepositoryException Indicates an irrecoverable error.
     */
    InputStream retrieveResource(String key) throws RepositoryException;
    
    /**
     * Retrieves a publically available URL to the resource, if available.
     * 
     * @param key The unique key of the resource.
     * @return A publically available URL to the resource or null if no such
     *         URL exists, or isnot publically available.
     * @throws RepositoryException Indicates an irrecoverable error.
     */
    URL retrieveURL(String key) throws RepositoryException;
    
    /**
     * Verifies that the resources exist in the repository.
     * 
     * @param resourceIds The list of resources to verify by ID.
     * @return 
     */
    Map<String, Boolean> verifyExistance(List<String> keys);

    /**
     * Deletes a resource from the repository;
     * 
     * @param key The unique key of the resource.
     * @throws RepositoryException Indicates an irrecoverable error.
     */
    void deleteResource(String key) throws RepositoryException;
}

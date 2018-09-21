package com.precisionhawk.poleams.repository;

/**
 *
 * @author pchapman
 */
public interface RepositoryConfig {
    
    public String getFSStorageDir();
   
    public String getS3BucketName();
    
    public String getRepositoryImplementation();
        
}

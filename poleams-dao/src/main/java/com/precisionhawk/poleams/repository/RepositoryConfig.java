package com.precisionhawk.poleams.repository;

/**
 *
 * @author Philip A. Chapman
 */
public interface RepositoryConfig {
    
    public String getFSStorageDir();
   
    public String getS3BucketName();
    
    public String getRepositoryImplementation();
        
}

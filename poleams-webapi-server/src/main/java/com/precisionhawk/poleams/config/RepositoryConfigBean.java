package com.precisionhawk.poleams.config;

import com.precisionhawk.poleams.repository.RepositoryConfig;

/**
 *
 * @author Philip A. Chapman
 */
public class RepositoryConfigBean implements RepositoryConfig {

    private String fsStorageDir;
    @Override
    public String getFSStorageDir() {
        return fsStorageDir;
    }
    public void setFSStorageDir(String dir) {
        this.fsStorageDir = dir;
    }

    private String s3BucketName;
    @Override
    public String getS3BucketName() {
        return s3BucketName;
    }
    public void setS3BucketName(String name) {
        this.s3BucketName = name;
    }

    private String repoImpl;
    @Override
    public String getRepositoryImplementation() {
        return repoImpl;
    }
    public void setRepositoryImplementation(String repoImpl) {
        this.repoImpl = repoImpl;
    }
    
}

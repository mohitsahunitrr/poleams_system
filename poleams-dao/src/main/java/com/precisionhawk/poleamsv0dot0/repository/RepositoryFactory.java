package com.precisionhawk.poleamsv0dot0.repository;

import com.precisionhawk.poleamsv0dot0.repository.filesystem.FSResourceRepository;
import com.precisionhawk.poleamsv0dot0.repository.s3.S3ResourceRepository;
import com.precisionhawk.poleamsv0dot0.support.aws.S3ClientFactory;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class RepositoryFactory implements Provider<ResourceRepository> {
    
    @Inject private S3ClientFactory s3ClientFactory;
    @Inject private RepositoryConfig config;

    private final Object LOCK = new Object();
    private ResourceRepository repository;
    
    @Override
    public ResourceRepository get() {
        synchronized (LOCK) {
            if (repository == null) {
                if (S3ResourceRepository.class.getName().equals(config.getRepositoryImplementation())) {
                    S3ResourceRepository rr = new S3ResourceRepository();
                    rr.setBucketName(config.getS3BucketName());
                    rr.setS3Client(s3ClientFactory.get());
                    repository = rr;
                } else if (FSResourceRepository.class.getName().equals(config.getRepositoryImplementation())) {
                    FSResourceRepository rr = new FSResourceRepository();
                    rr.setStorageDir(config.getFSStorageDir());
                    rr.init();
                    repository = rr;
                } else {
                    throw new IllegalArgumentException("Unknown or unset repository implementation.");
                }
            }
        }
        return repository;
    }
}

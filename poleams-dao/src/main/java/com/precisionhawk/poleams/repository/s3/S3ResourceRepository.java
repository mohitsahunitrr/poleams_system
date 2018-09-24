package com.precisionhawk.poleams.repository.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.repository.RepositoryException;
import com.precisionhawk.poleams.repository.ResourceRepository;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.papernapkin.liana.io.DeleteOnCloseFileInputStream;
import org.papernapkin.liana.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ResourceRepository which makes use of Amazon S3 for the
 * resource data and SimpleDB for easy search of the MetaData.
 *
 * @author Philip A. Chapman
 */
public class S3ResourceRepository implements ResourceRepository {

    private static final String META_POLE_ID = "poleams.pole.id";
    private static final String META_NAME = "poleams.resource.name";
    private static final String META_SUBSTATION_ID = "poleams.substation.id";
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final int MAX_IN_MEMORY_RESOURCE_SIZE = 2097152; // 2 MB
    
    private String bucketName;
    public String getBucketName() {
        return bucketName;
    }
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    private AmazonS3 s3Client;
    public AmazonS3 getS3Client() {
        return s3Client;
    }
    public void setS3Client(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }
    
    @PostConstruct
    public void initialize() {
        // Ensure bucket exists in S3
        boolean found = false;
        for (Bucket bucket : getS3Client().listBuckets()) {
            if (bucket.getName().equals(getBucketName())) {
                found = true;
                break;
            }
        }
        if (!found) {
            getS3Client().createBucket(getBucketName());
        }
    }
    
    @Override
    public void storeResource(ResourceMetadata metaData, String key, String name, String contentType, InputStream resourceStream, Long length) throws RepositoryException {
        ObjectMetadata ometa = new ObjectMetadata();
        ometa.setContentDisposition(StringUtil.replaceArgs("attachment; filename=\"{1}\"", name));
        ometa.setContentType(contentType);
        if (length != null) {
            ometa.setContentLength(length);
        }
        ometa.addUserMetadata(META_NAME, metaData.getName());
        ometa.addUserMetadata(META_POLE_ID, metaData.getPoleId());
        ometa.addUserMetadata(META_SUBSTATION_ID, metaData.getSubStationId());
        PutObjectRequest req = new PutObjectRequest(getBucketName(), key, resourceStream, ometa);
        req.setCannedAcl(CannedAccessControlList.PublicRead);
        getS3Client().putObject(req);
    }

    @Override
    public InputStream retrieveResource(String key) throws RepositoryException {
        S3Object object = null;
        try {
            object = getS3Client().getObject(new GetObjectRequest(getBucketName(), key));
        } catch (AmazonS3Exception ex) {
            if (ex.getStatusCode() == 404) {
                // Not found
                return null;
            } else {
                throw new RepositoryException(String.format("Error retrieving resource %s", key), ex);
            }
        }
        if (object == null) {
            LOGGER.error("Resource with key {} does not exist in S3 bucket {}", key, getBucketName());
            return new ByteArrayInputStream(new byte[0]);
        } else {
            InputStream is = null;
            OutputStream os = null;
            try {
                File tmp = null;
                // If the resource is small enough, keep it in memory.  Else
                // write it to a temporary file and then stream from that.
                if (object.getObjectMetadata().getContentLength() > MAX_IN_MEMORY_RESOURCE_SIZE) {
                    tmp = File.createTempFile("windams_", null);
                    os = new BufferedOutputStream(new FileOutputStream(tmp));
                } else {
                    os = new ByteArrayOutputStream();
                }
                is = object.getObjectContent();
                IOUtils.copy(is, os);
                is.close();
                os.close();
                if (tmp == null) {
                    return new ByteArrayInputStream(((ByteArrayOutputStream)os).toByteArray());
                } else {
                    return new DeleteOnCloseFileInputStream(tmp);
                }
            } catch (IOException e) {
                LOGGER.error("Error retrieving resource {}", key, e);
                throw new RepositoryException(StringUtil.replaceArgs("Unable to retrieve resource {1}", key), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {}
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    @Override
    public URL retrieveURL(String key) throws RepositoryException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        Date expiration = cal.getTime();
        try {
            return getS3Client().generatePresignedUrl(getBucketName(), key, expiration, HttpMethod.GET);
        } catch (AmazonClientException ex) {
            LOGGER.error("Unable to generate a presigned URL for key {}", key, ex);
            return null;
        }
    }

    @Override
    public Map<String, Boolean> verifyExistance(List<String> keys) {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        ObjectMetadata meta;
        for (String key : keys) {
            meta = null;
            try {
                meta = getS3Client().getObjectMetadata(getBucketName(), key);
            } catch (AmazonClientException ex) {
                LOGGER.debug("Unable to load metadata for key {}.  Not a problem.  Doesn't exist in repo.", key, ex);
            }
            results.put(key, meta != null);
        }
        return results;
    }

    @Override
    public void deleteResource(String key) throws RepositoryException {
        try {
            getS3Client().deleteObject(getBucketName(), key);
        } catch (AmazonClientException ex) {
            LOGGER.error("Unable to delete the resource for key {}", key, ex);
        }
    }
}

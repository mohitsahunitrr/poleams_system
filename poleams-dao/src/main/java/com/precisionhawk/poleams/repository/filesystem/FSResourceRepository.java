package com.precisionhawk.poleams.repository.filesystem;

import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.repository.RepositoryException;
import com.precisionhawk.poleams.repository.ResourceRepository;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.papernapkin.liana.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of ResourceRepository which stores everything on the
 * filesystem.
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class FSResourceRepository implements ResourceRepository {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    private String storageDirPath;
    public String getStorageDir() {
        return storageDirPath;
    }
    public void setStorageDir(String storageDir) {
        this.storageDirPath = storageDir;
    }
    
    private File storageDir;
    
    private File ensureStorageDir(String key) {
        File dir;
        if (key.length() > 4) {
            dir = new File(storageDir, key.substring(0, 3));
            if (!dir.exists()) {
                dir.mkdir();
            }
        } else {
            dir = storageDir;
        }
        return dir;
    }
    
    private File storageFile(String key) {
        return new File(ensureStorageDir(key), key);
    }
    
    private static final String DB_INIT_QUERY =
            "select count(*) from INFORMATION_SCHEMA.tables where TABLE_SCHEMA = 'PUBLIC'  and TABLE_NAME = 'RESOURCE_METADATA';";
    private static final String[] DB_INIT_UPDATES = {
        "create table RESOURCE_METADATA (KEY varchar(38) primary key, ORDER_NUMBER varchar(128) not null, SITE_ID varchar(128) not null, ASSET_ID varchar(256) not null,  FORM_ID varchar(38) not null, SUBMISSION_ID varchar(128) not null,  CONTENT_TYPE varchar(64) not null, NAME varchar(128) not null, SOURCE_URL varchar(256) not null);",
        "create index IDX_RESOURCE_METADATA_01 on RESOURCE_METADATA (ORDER_NUMBER);",
        "create index IDX_RESOURCE_METADATA_02 on RESOURCE_METADATA (SITE_ID);",
        "create index IDX_RESOURCE_METADATA_03 on RESOURCE_METADATA (ASSET_ID);",
        "create index IDX_RESOURCE_METADATA_04 on RESOURCE_METADATA (FORM_ID);",
        "create index IDX_RESOURCE_METADATA_05 on RESOURCE_METADATA (SUBMISSION_ID);"
    };
    
    @PostConstruct
    public void init() {
        // Ensure the storage directory exists
        storageDir = new File(getStorageDir());
        if (storageDir.exists()) {
            if (!storageDir.isDirectory() || !storageDir.canWrite()) {
                throw new IllegalArgumentException(StringUtil.replaceArgs("{1} is not a directory or not writable", storageDir.getAbsoluteFile()));
            }
        } else {
            if (!storageDir.mkdirs()) {
                throw new IllegalArgumentException(StringUtil.replaceArgs("Unable to create storage directory at {1}", storageDir.getAbsoluteFile()));
            }
        }
    }

    @Override
    public void storeResource(ResourceMetadata metaData, String key, String name, String contentType, InputStream resourceStream, Long length) throws RepositoryException {
        OutputStream os = null;
        try {
            // Store resource
            os = new BufferedOutputStream(new FileOutputStream(storageFile(key)));
            IOUtils.copy(resourceStream, os);
        } catch (IOException e) {
            throw new RepositoryException("Unable to store the resource", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {}
            }
            try {
                resourceStream.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public InputStream retrieveResource(String key) throws RepositoryException {
        File f = storageFile(key);
        if (f.exists() && f.canRead()) {
            try {
                return new FileInputStream(f);
            } catch (IOException e) {
                throw new RepositoryException(StringUtil.replaceArgs("Resource {1} does not exist in the repository or cannot be read.", key), e);
            }
        } else {
            throw new RepositoryException(StringUtil.replaceArgs("Resource {1} does not exist in the repository or cannot be read.", key));
        }
    }

    @Override
    public URL retrieveURL(String key) throws RepositoryException {
        // No such URL exists
        return null;
    }

    @Override
    public Map<String, Boolean> verifyExistance(List<String> keys) {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        File f;
        for (String key : keys) {
            f = storageFile(key);
            results.put(key, f.exists());
        }
        return results;
    }

    @Override
    public void deleteResource(String key) throws RepositoryException {
        File f = storageFile(key);
        if (f != null && f.exists()) {
            f.delete();
        }
    }
    
    public Long getLength(String key) {
        File f = storageFile(key);
        if (f.canRead()) {
            return f.length();
        } else {
            return null;
        }
    }
}

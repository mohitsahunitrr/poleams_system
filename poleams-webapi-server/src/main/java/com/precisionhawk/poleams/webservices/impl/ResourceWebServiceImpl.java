package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.service.ResourceService;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import java.util.List;
import java.util.Map;
import javax.ws.rs.InternalServerErrorException;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import javax.inject.Named;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class ResourceWebServiceImpl extends com.precisionhawk.ams.webservices.impl.ResourceWebServiceImpl implements ResourceWebService, ResourceService
{
    @Override
    public List<ResourceSummary> querySummaries(String authToken, ResourceSearchParams params) {
        return summaryFor(params);
    }
    
    List<ResourceSummary> summaryFor(ResourceSearchParams params) {
        if (params == null) {
            return Collections.emptyList();
        }
        try {
            List<ResourceMetadata> resources = resourceDao.lookup(params);
            if (resources == null || resources.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Search for and prepare possible thumbnails.
            Map<String, List<String>> thumbnailsBySourceId = new HashMap<>();
            params.setType(ResourceTypes.ThumbNail);
            List<String> ids;
            for (ResourceMetadata rmeta : resourceDao.lookup(params)) {
                if (rmeta.getSourceResourceId() != null) {
                    ids = thumbnailsBySourceId.get(rmeta.getSourceResourceId());
                    if (ids == null) {
                        ids = new LinkedList<>();
                        thumbnailsBySourceId.put(rmeta.getSourceResourceId(), ids);
                    }
                    ids.add(rmeta.getResourceId());
                }
            }

            // Prepare summary objects
            List<ResourceSummary> results = new ArrayList<>(resources.size());
            String thumbnailId;
            for (ResourceMetadata rmeta : resources) {
                thumbnailId = CollectionsUtilities.firstItemIn(thumbnailsBySourceId.get(rmeta.getResourceId()));
                results.add(new ResourceSummary(
                        rmeta,
                        getResourceDownloadURL(rmeta.getResourceId(), false),
                        getResourceDownloadURL(thumbnailId, false),
                        getResourceDownloadURL(rmeta.getZoomifyId(), true)
                ));
            }
            
            return results;
        } catch (DaoException dao) {
            throw new InternalServerErrorException("Error looking up thumbnail images.");
        }
    }
    
    ResourceSummary summaryFor(ResourceMetadata rmeta) {
        if (rmeta == null) {
            return null;
        }
        
        String downloadURL = getResourceDownloadURL(rmeta.getResourceId(), false);
        String zoomifyURL = null;
        if (rmeta.getZoomifyId() != null) {
            zoomifyURL = getResourceDownloadURL(rmeta.getZoomifyId(), true);
        }
        
        // Find scaled image, if any.
        String scaledImageURL = null;
        if (ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) != null) {
            // Only images are scaled.
            ResourceSearchParams params = new ResourceSearchParams();
            params.setSourceResourceId(rmeta.getResourceId());
            params.setType(ResourceTypes.ThumbNail);
            try {
                ResourceMetadata thumbnail = CollectionsUtilities.firstItemIn(resourceDao.lookup(params));
                if (thumbnail != null) {
                    scaledImageURL = getResourceDownloadURL(thumbnail.getResourceId(), false);
                }
            } catch (DaoException dao) {
                throw new InternalServerErrorException("Error looking up thumbnail images.", dao);
            }
        }
        
        return new ResourceSummary(rmeta, downloadURL, scaledImageURL, zoomifyURL);
    }

    public String getResourceDownloadURL(String resourceId) {
        return super.getResourceDownloadURL(resourceId, false);
    }
}

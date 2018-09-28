package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.Dimension;
import com.precisionhawk.poleams.bean.ImageScaleRequest;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.config.ServicesConfig;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.ResourceMetadataDao;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.repository.RepositoryException;
import com.precisionhawk.poleams.repository.ResourceRepository;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class ResourceWebServiceImpl extends AbstractWebService implements ResourceWebService {

    @Inject private ResourceMetadataDao resourceDao;
    @Inject private ResourceRepository repo;
    @Inject private ServicesConfig config;
    
    @Override
    public void delete(String authToken, String resourceId) {
        ensureExists(resourceId, "The resource ID is required.");
        try {
            ResourceMetadata rmeta = resourceDao.retrieveResourceMetadata(resourceId);
            if (rmeta == null) {
                ResourceSearchParameters params = new ResourceSearchParameters();
                params.setZoomifyId(resourceId);
                rmeta = CollectionsUtilities.firstItemIn(resourceDao.lookup(params));
                if (rmeta == null) {
                    return;
                } // Else, this is a zoomify file, remove it from the repository.
            } else {
                resourceDao.deleteMetadata(resourceId);
            }
            repo.deleteResource(resourceId);
        } catch (DaoException | RepositoryException ex) {
            throw new InternalServerErrorException(String.format("Error deleting resource %s", resourceId));
        }
    }

    @Override
    public ResourceMetadata retrieve(String authToken, String resourceId) {
        ensureExists(resourceId, "The resource ID is required.");
        try {
            return resourceDao.retrieveResourceMetadata(resourceId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException(String.format("Error retrieving resource %s", resourceId));
        }
    }

    @Override
    public List<ResourceMetadata> query(String authToken, ResourceSearchParameters params) {
        ensureExists(params, "The search parameters are required.");
        try {
            return resourceDao.lookup(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving resources by search parameters.");
        }
    }

    @Override
    public ResourceMetadata scale(String authToken, String resourceId, ImageScaleRequest scaleRequest) {
        ensureExists(resourceId, "The resource ID is required.");
        ensureExists(scaleRequest, "The image scale request is required.");
        try {
            ResourceMetadata rmeta = resourceDao.retrieveResourceMetadata(resourceId);
            if (rmeta == null) {
                throw new NotFoundException(String.format("No image %s found.", resourceId));
            } else if (!rmeta.getContentType().startsWith("image/")) {
                throw new BadRequestException(String.format("The resource %s is not an image.", resourceId));
            }
            return createScaledImageFromOriginal(rmeta, scaleRequest);
        } catch (DaoException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public Map<String, Boolean> verifyUploadedResources(String authToken, List<String> resourceIDs) {
        ensureExists(resourceIDs, "The resource IDs are required.");
        return repo.verifyExistance(resourceIDs);
    }

    @Override
    public ResourceMetadata insertResourceMetadata(String authToken, ResourceMetadata rmeta) {
        ensureExists(rmeta, "The resource metadata is required.");
        if (rmeta.getResourceId() == null) {
            rmeta.setResourceId(UUID.randomUUID().toString());
        }
        try {
            if (resourceDao.insertMetadata(rmeta)) {
                LOGGER.debug("Resource {} has been inserted.", rmeta.getResourceId());
                return rmeta;
            } else {
                throw new BadRequestException(String.format("Metadata for resource %s already exists.", rmeta.getResourceId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole.", ex);
        }
    }

    @Override
    public ResourceMetadata updateResourceMetadata(String authToken, ResourceMetadata rmeta) {
        ensureExists(rmeta, "The resource metadata is required.");
        ensureExists(rmeta.getResourceId(), "The resource ID is required.");
        try {
            if (resourceDao.updateMetadata(rmeta)) {
                LOGGER.debug("Resource {} has been updated.", rmeta.getResourceId());
                return rmeta;
            } else {
                throw new NotFoundException(String.format("No metadata for resource %s exists.", rmeta.getResourceId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole.", ex);
        }
    }

    @Override
    public Response downloadResource(String resourceId) {
        ensureExists(resourceId, "The resource IDs are required.");
        try {
            boolean isZoomify = false;
            ResourceMetadata rmeta = resourceDao.retrieveResourceMetadata(resourceId);
            if (rmeta == null) {
                // This may be zoomify resource.
                ResourceSearchParameters params = new ResourceSearchParameters();
                params.setZoomifyId(resourceId);
                rmeta = CollectionsUtilities.firstItemIn(resourceDao.lookup(params));
                if (rmeta == null) {
                    throw new NotFoundException(String.format("No resource with ID %s exists.", resourceId));
                } else {
                    isZoomify = true;
                }
            }
            // If we reached here, resourceId is a valid resource or zoomify ID.
            URL redirect = repo.retrieveURL(resourceId);
            if (redirect == null) {
                return provideResource(rmeta, isZoomify);
            } else {
                return Response.status(302).header("location", redirect).build();
                // The below returns a 307
//                return Response.temporaryRedirect(redirect.toURI()).build();
            }
        } catch (DaoException | RepositoryException ex) { // | URISyntaxException ex) {
            throw new InternalServerErrorException(String.format("Error retrieving resource %s", resourceId));
        }
    }
    
    private Response provideResource(final ResourceMetadata rmeta, final boolean isZoomify) {
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                InputStream is = null;
                String key = isZoomify ? rmeta.getZoomifyId() : rmeta.getResourceId();
                try {
                    is = repo.retrieveResource(key);
                    IOUtils.copy(is, output);
                } catch (RepositoryException ex) {
                    LOGGER.error("Error retrieving resource {}", key, ex);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        };
        String contentType;
        String fileNameHeader;
        if (isZoomify) {
            contentType = "image/zif";
            fileNameHeader = String.format("attachment; filename=\"%s.zif\"", rmeta.getZoomifyId());
        } else {
            contentType = rmeta.getContentType();
            fileNameHeader = String.format("attachment; filename=\"%s\"", rmeta.getName());
        }
        return Response.ok(stream, contentType).header("content-disposition", fileNameHeader).build();
    }

    @Override
    public void uploadResource(String authToken, String resourceId, HttpServletRequest req) {
        ensureExists(resourceId, "Resource ID is required.");
        try {
            String contentType;
            String name;
            ResourceMetadata meta = resourceDao.retrieveResourceMetadata(resourceId);
            if (meta == null) {
                // It may be a zoomify image.
                ResourceSearchParameters rparms = new ResourceSearchParameters();
                rparms.setZoomifyId(resourceId);
                meta = CollectionsUtilities.firstItemIn(resourceDao.lookup(rparms));
                if (meta == null) {
                    LOGGER.debug("No metadata for resource {}, upload aborted.", resourceId);
                    throw new BadRequestException(String.format("No metadata for resource %s found.  Data cannot be uploaded.", resourceId));
                } else {
                    // Zoomify image
                    contentType = "image/zif";
                    name = resourceId + ".zif";
                }
            } else {
                contentType = meta.getContentType();
                name = meta.getName();
            }
                    
            if (ServletFileUpload.isMultipartContent(req)) {
                LOGGER.debug("Data being uploaded for resource {}", resourceId);
                
                // Configure a repository (to ensure a secure temp location is used)
                ServletContext servletContext = req.getSession().getServletContext();
                File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
                DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
                fileItemFactory.setRepository(repository);

                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(fileItemFactory);

                // Parse the request
                List<FileItem> items = upload.parseRequest(req);
                if (items.size() != 1) {
                    LOGGER.error("Multiple parts uploaded for resource {}", resourceId);
                    throw new BadRequestException("It is expected that exactly 1 file will be uploaded");
                } else {
                    FileItem fileitem = items.get(0);
                    // Update metadata
                    InputStream is = null;
                    try {
                        is = fileitem.getInputStream();
                        repo.storeResource(meta, resourceId, name, contentType, is, null);
                        LOGGER.debug("Data for resource {} stored", resourceId);
                        LOGGER.debug("Content type {} and name {} for resource {} stored", contentType, name, resourceId);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ioe) {}
                        }
                    }
                }
            } else {
                repo.storeResource(meta, resourceId, name, contentType, req.getInputStream(), null);
            }
        } catch (DaoException | RepositoryException | IOException | FileUploadException ex) {
            throw new InternalServerErrorException(String.format("Unable to store resource %s", resourceId));
        }
    }
    
    private ResourceMetadata createScaledImageFromOriginal(ResourceMetadata originalMD, ImageScaleRequest scaleRequest)
//        throws ServiceException
    {
        ResourceMetadata destMD = null;
        try {
            if (ImageUtilities.ImageType.fromContentType(originalMD.getContentType()) != null)
            {
                byte[] bytes;
                InputStream is = null;
                OutputStream os = null;
                File tmpFile = null;
                try {
                    // Load the image
                    is = repo.retrieveResource(originalMD.getResourceId());
                    if (is == null) {
                        throw new NotFoundException(String.format("The image %s does not exist in the repository.", originalMD.getResourceId()));
                    }
                    tmpFile = File.createTempFile("windams", "image");
                    os = new BufferedOutputStream(new FileOutputStream(tmpFile));
                    IOUtils.copy(is, os);
                    is.close();
                    is = null;
                    os.close();
                    os = null;
                    Scalr.Mode mode = null;
                    if (null != scaleRequest.getScaleOperation()) {
                        switch (scaleRequest.getScaleOperation()) {
                            case ScaleToFit:
                                mode = Scalr.Mode.AUTOMATIC;
                                break;
                            case ScaleToHeight:
                                mode = Scalr.Mode.FIT_TO_HEIGHT;
                                break;
                            case ScaleToSize:
                                mode = Scalr.Mode.FIT_EXACT;
                                break;
                            case ScaleToWidth:
                                mode = Scalr.Mode.FIT_TO_WIDTH;
                                break;
                            default:
                                break;
                        }
                    }
                    if (mode == null) {
                        throw new BadRequestException("Invalid scale operation.");
                    }
                    BufferedImage srcImage = ImageIO.read(tmpFile); // Load image
                    BufferedImage scaledImage = Scalr.resize(srcImage, Scalr.Method.AUTOMATIC, mode, scaleRequest.getWidth().intValue(), scaleRequest.getHeight().intValue());
                    os = new ByteArrayOutputStream();
                    ImageIO.write(scaledImage, scaleRequest.getResultType().name(), os);
                    os.close();
                    bytes = ((ByteArrayOutputStream)os).toByteArray();
                    os = null;
                    destMD = new ResourceMetadata();
                    destMD.setPoleId(originalMD.getPoleId());
                    destMD.setPoleInspectionId(originalMD.getPoleInspectionId());
                    destMD.setOrganizationId(originalMD.getOrganizationId());
                    destMD.setSubStationId(originalMD.getSubStationId());
                    destMD.setContentType(ImageUtilities.ImageType.fromExtension(scaleRequest.getResultType().name()).getContentType());
                    destMD.setLocation(originalMD.getLocation());
                    destMD.setName(originalMD.getName());
                    destMD.setResourceId(UUID.randomUUID().toString());
                    destMD.setSize(new Dimension(Double.valueOf(scaledImage.getWidth()), Double.valueOf(scaledImage.getHeight())));
                    destMD.setSourceResourceId(originalMD.getResourceId());
                    destMD.setStatus(ResourceStatus.Released);
                    destMD.setTimestamp(originalMD.getTimestamp());
                    destMD.setType(ResourceType.ThumbNail);
                    resourceDao.insertMetadata(destMD);
                    repo.storeResource(destMD, destMD.getResourceId(), destMD.getName(), destMD.getContentType(), new ByteArrayInputStream(bytes), Long.valueOf(bytes.length));
                } catch (DaoException daoe) {
                    throw new InternalServerErrorException("Error saving scaled image", daoe);
                } catch (IOException ioe) {
                    throw new InternalServerErrorException(String.format("Error scaling the resource %s.", originalMD.getResourceId()), ioe);
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                    if (tmpFile != null && tmpFile.exists()) {
                        tmpFile.delete();
                    }
                }
            } else {
                throw new InternalServerErrorException(String.format("Resource with mime type %s is not supported for scaling.", originalMD.getContentType()));
            }
        } catch (RepositoryException re) {
            throw new InternalServerErrorException(String.format("Error retrieving the resource %s.", originalMD.getResourceId()), re);
        }
        return destMD;
    }
    
    private static final String DOWNLOAD_PATH = "%s/resource/%s/download";
    
    String getResourceDownloadURL(String resourceId, boolean isZoomify) {
        if (resourceId == null || resourceId.isEmpty()) {
            return null;
        }
        String url = null;
        if (isZoomify) {
            // Try to go directly to source.  This is necessary for the S3 repo.
            try {
                url = repo.retrieveURL(resourceId).toExternalForm();
            } catch (RepositoryException ex) {
                LOGGER.error("Error determining direct access URL for resource {}", resourceId, ex);
            }
        }
        if (url == null) {
            url = String.format(DOWNLOAD_PATH, config.getServicesURL(), resourceId);
        }
        return url;
    }
    
    public List<ResourceSummary> querySummaries(String authToken, ResourceSearchParameters params) {
        return summaryFor(params);
    }
    
    List<ResourceSummary> summaryFor(ResourceSearchParameters params) {
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
            params.setType(ResourceType.ThumbNail);
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
            ResourceSearchParameters params = new ResourceSearchParameters();
            params.setSourceResourceId(rmeta.getResourceId());
            params.setType(ResourceType.ThumbNail);
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
}

package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ImageScaleRequest;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ContentTypeUtilities;
import com.precisionhawk.ams.util.ImageUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionLineWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.jboss.resteasy.client.ClientResponseFailure;

/**
 *
 * @author pchapman
 */
public class ResourceUploadProcess extends ServiceClientCommandProcess {
    
    private static final double SCALE_WIDTH = 100;
    private static final ImageScaleRequest SCALE_IMAGE_REQ;
    static {
        SCALE_IMAGE_REQ = new ImageScaleRequest();
        SCALE_IMAGE_REQ.setResultType(ImageScaleRequest.ContentType.JPEG);
        SCALE_IMAGE_REQ.setScaleOperation(ImageScaleRequest.ScaleOperation.ScaleToWidth);
        SCALE_IMAGE_REQ.setHeight(0.0);
        SCALE_IMAGE_REQ.setWidth(SCALE_WIDTH);
    }
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_FPL_ID = "-fplid";
    private static final String ARG_LINE_ID = "-line";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_RESOURCE_ID = "-resourceId";
    private static final String ARG_REPLACE = "-replace";
    private static final String ARG_STRUCT = "-struct";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "uploadResource";

    private static final String HELP =
            "\t" + COMMAND
            + " [" + ARG_FEEDER_ID + " FeederId]"
            + " [" + ARG_FPL_ID + " FPL_Id]"
            + " [" + ARG_ORDER_NUM + " WorkOrderNum"
            + " [" + ARG_RESOURCE_ID + " ResourceId] "
            + " [" + ARG_TYPE + " ResourceType]"
            + " [" + ARG_REPLACE + "]"
            + " path/to/resource";

    private String contentType;
    private String feederId;
    private String fplId;
    private String fileName;
    private String lineId;
    private String orderNum;
    private boolean replace = false;
    private String resourceId;
    private ResourceType resourceType;
    private String struct;
    
    public ResourceUploadProcess() {}
    
    ResourceUploadProcess(String feederId, String orderNum, String fplId, String resourceId, ResourceType resourceType, boolean replace, String fileName, String contentType) {
        this.contentType = contentType;
        this.feederId = feederId;
        this.fileName = fileName;
        this.fplId = fplId;
        this.orderNum = orderNum;
        this.replace = replace;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_FEEDER_ID:
                if (feederId == null) {
                    feederId = args.poll();
                    return feederId != null;
                } else {
                    return false;
                }
            case ARG_FPL_ID:
                if (fplId == null) {
                    fplId = args.poll();
                    return fplId != null;
                } else {
                    return false;
                }
            case ARG_LINE_ID:
                if (lineId == null) {
                    lineId = args.poll();
                    return lineId != null;
                } else {
                    return false;
                }
            case ARG_ORDER_NUM:
                if (orderNum == null) {
                    orderNum = args.poll();
                    return orderNum != null;
                } else {
                    return false;
                }
            case ARG_REPLACE:
                if (replace) {
                    return false;
                } else {
                    replace = true;
                    return true;
                }
            case ARG_RESOURCE_ID:
                if (resourceId == null) {
                    resourceId = args.poll();
                    return resourceId != null;
                } else {
                    return false;
                }
            case ARG_STRUCT:
                if (struct == null) {
                    struct = args.poll();
                    return struct != null;
                } else {
                    return false;
                }
            case ARG_TYPE:
                if (resourceType == null) {
                    resourceType = ResourceTypes.valueOf(args.poll());
                    return resourceType != null;
                } else {
                    return false;
                }
            default:
                if (fileName == null) {
                    fileName = arg;
                    return true;
                } else {
                    return false;
                }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (fileName == null) {
            return false;
        }
        File f = new File(fileName);
        String fn = f.getName().toUpperCase();
        if (!f.canRead()) {
            System.err.printf("The file \"%s\" either does not exist or cannot be read.\n", f);
        }
        
        if (contentType == null) {
            contentType = ContentTypeUtilities.guessContentType(f);
        }
        if (contentType == null) {
            System.err.printf("The file \"%s\" is an unrecognized file type.", f);
        } else {
            try {
                ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);

                ResourceMetadata rmeta = null;
                
                if (resourceId == null) {
                    Feeder feeder = null;
                    FeederInspection feederInspection = null;
                    WorkOrder workOrder = null;
                    Pole pole = null;
                    PoleInspection poleInspection = null;
                    TransmissionLine line = null;
                    TransmissionLineInspection tlinsp = null;
                    TransmissionStructure structure = null;
                    TransmissionStructureInspection tsinsp = null;
                    
                    if (feederId != null) {
                        // Search for the feeder;
                        FeederSearchParams params = new FeederSearchParams();
                        params.setFeederNumber(feederId);
                        List<Feeder> list = env.obtainWebService(FeederWebService.class).search(env.obtainAccessToken(), params);
                        feeder = CollectionsUtilities.firstItemIn(list);
                        if (feeder == null) {
                            System.err.printf("No feeder %s found\n", feederId);
                            return true;
                        }
                    }
                    
                    if (lineId != null) {
                        // Search for the line;
                        TransmissionLineSearchParams params = new TransmissionLineSearchParams();
                        params.setLineNumber(lineId);
                        line = CollectionsUtilities.firstItemIn(env.obtainWebService(TransmissionLineWebService.class).search(env.obtainAccessToken(), params));
                        if (line == null) {
                            System.err.printf("No Transmission Structure %s found\n", lineId);
                            return true;
                        }
                    }
                    
                    if (orderNum != null) {
                        workOrder = env.obtainWebService(WorkOrderWebService.class).retrieveById(env.obtainAccessToken(), orderNum);
                        if (workOrder == null) {
                            System.err.printf("No work order found for order number %s\n", orderNum);
                            return true;
                        }
                        if (workOrder.getSiteIds().isEmpty()) {
                            System.err.printf("The work order %s has no feeders related to it.\n", orderNum);
                            return true;
                        }
                        if (feeder == null || line == null) {
                            if (workOrder.getSiteIds().size() == 1) {
                                feederId = workOrder.getSiteIds().get(0);
                                try {
                                    feeder = env.obtainWebService(FeederWebService.class).retrieve(env.obtainAccessToken(), feederId);
                                } catch (ClientResponseFailure ex) {
                                    if (ex.getResponse().getStatus() == 404) {
                                        feeder = null;
                                    } else {
                                        throw ex;
                                    }
                                }
                                if (feeder == null) {
                                    try {
                                        line = env.obtainWebService(TransmissionLineWebService.class).retrieve(env.obtainAccessToken(), feederId);
                                    } catch (ClientResponseFailure ex) {
                                        if (ex.getResponse().getStatus() == 404) {
                                            line = null;
                                        } else {
                                            throw ex;
                                        }
                                    }
                                    if (line == null) {
                                        System.err.printf("No feeder or line found %s found\n", feederId);
                                        return true;
                                    }
                                }
                            } else {
                                System.err.printf("There are multiple feeders or lines associated with work order %s.  No idea which one to assign the resource to.\n", orderNum);
                                return true;
                            }
                        } else {
                            boolean found = false;
                            for (String id : workOrder.getSiteIds()) {
                                if (feeder.getId().equals(id)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                System.err.printf("The work order %s is not related to the feeder %s.\n", orderNum, feeder.getFeederNumber());
                                return true;
                            }
                        }
                        SiteInspectionSearchParams params = new SiteInspectionSearchParams();
                        params.setOrderNumber(orderNum);
                        if (feeder != null) {
                            params.setSiteId(feeder.getId());
                            List<FeederInspection> list = env.obtainWebService(FeederInspectionWebService.class).search(env.obtainAccessToken(), params);
                            feederInspection = CollectionsUtilities.firstItemIn(list);
                        } else {
                            params.setSiteId(line.getId());
                            tlinsp = CollectionsUtilities.firstItemIn(env.obtainWebService(TransmissionLineInspectionWebService.class).search(env.obtainAccessToken(), params));
                        }
                    }
                    
                    if (fplId != null) {
                        PoleSearchParams params = new PoleSearchParams();
                        params.setUtilityId(fplId);
                        if (feeder != null) {
                            params.setSiteId(feeder.getId());
                        }
                        List<Pole> list = env.obtainWebService(PoleWebService.class).search(env.obtainAccessToken(), params);
                        pole = CollectionsUtilities.firstItemIn(list);
                        if (pole == null) {
                            System.err.printf("No pole found for Utility ID %s and Feeder ID %s", fplId, feeder == null ? null : feeder.getFeederNumber());
                            return true;
                        }
                    }
                    
                    if (struct != null) {
                        TransmissionStructureSearchParams params = new TransmissionStructureSearchParams();
                        params.setStructureNumber(struct);
                        List<TransmissionStructure> list = env.obtainWebService(TransmissionStructureWebService.class).search(env.obtainAccessToken(), params);
                        if (list.isEmpty()) {
                            System.err.printf("No transmission structure %s found\n", struct);
                            return true;
                        } else if (list.size() == 1) {
                            structure = list.get(0);
                        } else {
                            System.err.printf("Multiple transmission structures %s found\n", struct);
                            return true;
                        }
                    }
                    
                    if (workOrder != null) {
                        if (pole != null) {
                            AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                            params.setAssetId(pole.getId());
                            params.setOrderNumber(workOrder.getOrderNumber());
                            List<PoleInspection> list = env.obtainWebService(PoleInspectionWebService.class).search(env.obtainAccessToken(), params);
                            poleInspection = CollectionsUtilities.firstItemIn(list);
                        } else if (structure != null) {
                            AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                            params.setAssetId(structure.getId());
                            params.setOrderNumber(workOrder.getOrderNumber());
                            List<TransmissionStructureInspection> list = env.obtainWebService(TransmissionStructureInspectionWebService.class).search(env.obtainAccessToken(), params);
                            tsinsp = CollectionsUtilities.firstItemIn(list);
                        }
                    }
                    
                    ResourceSearchParams params = new ResourceSearchParams();
                    Class<?> clazz = ResourceTypes.relatedTo(resourceType);
                    if (AssetInspection.class == clazz) {
                        AssetInspection insp = poleInspection;
                        if (insp == null) {
                            insp = tsinsp;
                        }
                        if (insp == null) {
                            System.err.println("Pole or Transmission structure is required for this type of resource.");
                        }
                        params.setAssetId(insp.getAssetId());
                        params.setAssetInspectionId(insp.getId());
                        params.setOrderNumber(insp.getOrderNumber());
                        params.setSiteId(insp.getSiteId());
                        params.setSiteInspectionId(insp.getSiteInspectionId());
                    } else if (Feeder.class == clazz) {
                        if (feeder == null) {
                            System.err.println("Feeder is required for this type of resource.");
                            return true;
                        } else {
                            params.setSiteId(feeder.getId());
                        }
                    } else if (FeederInspection.class == clazz) {
                        if (feederInspection == null) {
                            System.err.println("Feeder inspection is required for this type of resource.");
                            return true;
                        } else {
                            params.setOrderNumber(feederInspection.getOrderNumber());
                            params.setSiteId(feederInspection.getSiteId());
                            params.setSiteInspectionId(feederInspection.getId());
                        }
                    } else if (Pole.class == clazz) {
                        if (pole == null) {
                            System.err.println("Pole is required for this type of resource.");
                            return true;
                        } else {
                            params.setAssetId(pole.getId());
                            params.setSiteId(pole.getSiteId());
                        }
                    } else if (PoleInspection.class == clazz) {
                        if (poleInspection == null) {
                            System.err.println("Pole inspection is required for this type of resource.");
                            return true;
                        } else {
                            params.setAssetId(poleInspection.getAssetId());
                            params.setAssetInspectionId(poleInspection.getId());
                            params.setOrderNumber(poleInspection.getOrderNumber());
                            params.setSiteId(poleInspection.getSiteId());
                            params.setSiteInspectionId(poleInspection.getSiteInspectionId());
                        }
                    } else if (TransmissionLine.class == clazz) {
                        if (line == null) {
                            System.err.println("Transmission line required for this type of resource.");
                        } else {
                            params.setSiteId(line.getId());
                            params.setSiteInspectionId(tlinsp.getId());
                            params.setOrderNumber(tlinsp.getOrderNumber());
                        }
                    } else if (TransmissionStructure.class == clazz) {
                        if (structure == null) {
                            System.err.print("Transmission structure required for this type of resource.");
                        } else {
                            params.setAssetId(structure.getId());
                            params.setAssetInspectionId(tsinsp.getId());
                            params.setOrderNumber(tsinsp.getOrderNumber());
                            params.setSiteId(structure.getSiteId());
                            params.setSiteInspectionId(tsinsp.getSiteInspectionId());
                        }
                    }

                    // See if the resource already exist for the data objects.
                    
                    if (params.hasCriteria()) {
                        // Look for the resource metadata
                        rmeta = CollectionsUtilities.firstItemIn(rsvc.query(env.obtainAccessToken(), params));
                        if (rmeta == null) {
                            // None found, create it.
                            rmeta = new ResourceMetadata();
                            rmeta.setOrderNumber(params.getOrderNumber());
                            rmeta.setAssetId(params.getAssetId());
                            rmeta.setAssetInspectionId(params.getAssetInspectionId());
                            rmeta.setSiteId(params.getSiteId());
                            rmeta.setSiteInspectionId(params.getSiteInspectionId());
                            rmeta.setType(resourceType);
                            rmeta.setContentType(contentType);
                            rmeta.setName(f.getName());
                            rmeta.setResourceId(UUID.randomUUID().toString());
                            rmeta.setStatus(ResourceStatus.QueuedForUpload);
                            rmeta.setTimestamp(ZonedDateTime.now());
                            rsvc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
                        }
                    } else {
                        System.err.println("No search parameters for finding a correct resource.");
                        return true;
                    }
                } else {
                    rmeta = rsvc.retrieve(env.obtainAccessToken(), resourceId);
                    if (rmeta == null) {
                        System.err.printf("No resource with ID \"%s\" found in data store.\n", resourceId);
                        return true;
                    }
                }

                if (ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) != null) {
                    ImageInfo info = Imaging.getImageInfo(f);
                    TiffImageMetadata exif;
                    ImageMetadata metadata = Imaging.getMetadata(f);
                    if (metadata instanceof JpegImageMetadata) {
                        exif = ((JpegImageMetadata)metadata).getExif();
                    } else if (metadata instanceof TiffImageMetadata) {
                        exif = (TiffImageMetadata)metadata;
                    } else {
                        exif = null;
                    }
                    rmeta.setLocation(ImageUtilities.getLocation(exif));
                    rmeta.setSize(ImageUtilities.getSize(info));
                }

                HttpClientUtilities.postFile(env, rmeta.getResourceId(), contentType, f);

                if (resourceId == null) {
                    if (ResourceTypes.IdentifiedComponents.equals(rmeta.getType())) {
                        rmeta.setStatus(ResourceStatus.Processed);
                    } else {
                        // If this is a new upload rather than a re-upload, switch status to Released.
                        rmeta.setStatus(ResourceStatus.Released);
                    }
                    rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                }
                
                if (
                        ImageUtilities.ImageType.fromContentType(rmeta.getContentType()) != null
                        && rmeta.getSize() != null
                        && rmeta.getSize().getWidth() > SCALE_WIDTH
                    )
                {
                    ResourceMetadata rm2 = rsvc.scale(env.obtainAccessToken(), rmeta.getResourceId(), SCALE_IMAGE_REQ);
                    rm2.setType(ResourceTypes.ThumbNail);
                    rsvc.updateResourceMetadata(env.obtainAccessToken(), rm2);
                }
                
                System.out.printf("The file \"%s\" of type %s has been uploaded to %s with resourceId %s\n", f, resourceType, feederId, rmeta.getResourceId());
            } catch (ImageReadException | IOException | URISyntaxException ex) {
                ex.printStackTrace(System.err);
            }
        }

        return true;
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
        output.println("\t\tResource Types:");
        for (ResourceType type : ResourceTypes.values()) {
            output.printf("\t\t\t%s\n", type.getValue());
        }
    }
    
}

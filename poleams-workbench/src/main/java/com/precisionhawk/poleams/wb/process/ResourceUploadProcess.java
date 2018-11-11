package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ContentTypeUtilities;
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
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Queue;
import java.util.UUID;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.WorkOrderWebService;
import java.util.List;

/**
 *
 * @author pchapman
 */
public class ResourceUploadProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_FPL_ID = "-fplid";
    private static final String ARG_ORDER_NUM = "-orderNum";
    private static final String ARG_RESOURCE_ID = "-resourceId";
    private static final String ARG_REPLACE = "-replace";
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
    private String orderNum;
    private boolean replace = false;
    private String resourceId;
    private ResourceType resourceType;
    
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
        if ((resourceId == null && feederId == null && fplId == null) || fileName == null) {
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
                    
                    if (orderNum != null) {
                        workOrder = env.obtainWebService(WorkOrderWebService.class).retrieveById(env.obtainAccessToken(), orderNum);
                        if (workOrder == null) {
                            System.err.printf("No work order found for order number %s\n", orderNum);
                            return true;
                        }
                        if (feeder == null) {
                            feeder = env.obtainWebService(FeederWebService.class).retrieve(env.obtainAccessToken(), workOrder.getSiteId());
                        }
                        SiteInspectionSearchParams params = new SiteInspectionSearchParams();
                        params.setOrderNumber(orderNum);
                        params.setSiteId(workOrder.getSiteId());
                        List<FeederInspection> list = env.obtainWebService(FeederInspectionWebService.class).search(env.obtainAccessToken(), params);
                        feederInspection = CollectionsUtilities.firstItemIn(list);
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
                    
                    if (workOrder != null && pole != null) {
                        AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                        params.setAssetId(pole.getId());
                        params.setOrderNumber(workOrder.getOrderNumber());
                        List<PoleInspection> list = env.obtainWebService(PoleInspectionWebService.class).search(env.obtainAccessToken(), params);
                        poleInspection = CollectionsUtilities.firstItemIn(list);
                    }
                    
                    ResourceSearchParams params = new ResourceSearchParams();
                    Class<?> clazz = ResourceTypes.relatedTo(resourceType);
                    if (Feeder.class == clazz) {
                        if (feeder == null) {
                            System.err.println("Feeder is required for this type of resource.");
                            return true;
                        } else {
                            params.setSiteId(feeder.getId());
                        }
                    } else if (FeederInspection.class == clazz) {
                        if (feederInspection == null) {
                            System.err.println("Feeder inspection is required for htis type of resource.");
                            return true;
                        } else {
                            params.setOrderNumber(feederInspection.getOrderNumber());
                            params.setSiteId(feederInspection.getSiteId());
                            params.setSiteInspectionId(feederInspection.getId());
                        }
                    } else if (Pole.class == clazz) {
                        if (pole == null) {
                            System.err.println("Pole is required for htis type of resource.");
                            return true;
                        } else {
                            params.setAssetId(pole.getId());
                            params.setSiteId(pole.getSiteId());
                        }
                    } else if (PoleInspection.class == clazz) {
                        if (poleInspection == null) {
                            System.err.println("Pole inspection is required for htis type of resource.");
                            return true;
                        } else {
                            params.setAssetId(poleInspection.getAssetId());
                            params.setAssetInspectionId(poleInspection.getId());
                            params.setOrderNumber(poleInspection.getOrderNumber());
                            params.setSiteId(poleInspection.getSiteId());
                            params.setSiteInspectionId(poleInspection.getSiteInspectionId());
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
                            rmeta.setType(params.getType());
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
                uploadFile(env, rmeta, contentType, f);
                if (ResourceStatus.QueuedForUpload == rmeta.getStatus()) {
                    rmeta.setStatus(ResourceStatus.Released);
                }
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace(System.err);
            }
        }

        return true;
    }
    
    private boolean uploadFile(Environment env, ResourceMetadata rmeta, String contentType, File f)
        throws IOException, URISyntaxException
    {
        HttpClientUtilities.postFile(env, rmeta.getResourceId(), contentType, f);

        if (resourceId == null && ResourceStatus.Released != rmeta.getStatus()) {
            // If this is a new upload rather than a re-upload, switch status to Released.
            rmeta.setStatus(ResourceStatus.Released);
            env.obtainWebService(ResourceWebService.class).updateResourceMetadata(env.obtainAccessToken(), rmeta);
        }

        System.out.printf("The file \"%s\" of type %s has been uploaded to %s with resourceId %s\n", f, resourceType, feederId, rmeta.getResourceId());
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

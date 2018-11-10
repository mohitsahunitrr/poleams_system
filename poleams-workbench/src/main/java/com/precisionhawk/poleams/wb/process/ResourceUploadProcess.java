package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.domain.ResourceType;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.ams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.util.ContentTypeUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Queue;
import java.util.UUID;
import com.precisionhawk.poleams.webservices.FeederWebService;

/**
 *
 * @author pchapman
 */
public class ResourceUploadProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_FPL_ID = "-fplid";
    private static final String ARG_RESOURCE_ID = "-resourceId";
    private static final String ARG_REPLACE = "-replace";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "uploadResource";

    private static final String HELP =
            "\t" + COMMAND
            + " [" + ARG_FEEDER_ID + " FeederId]"
            + " [" + ARG_FPL_ID + " FPL_Id]"
            + " [" + ARG_RESOURCE_ID + " ResourceId] "
            + " [" + ARG_TYPE + " ResourceType]"
            + " [" + ARG_REPLACE + "]"
            + " path/to/resource";

    private String contentType;
    private String feederId;
    private String fplId;
    private String fileName;
    private boolean replace = false;
    private String resourceId;
    private ResourceType resourceType;
    
    public ResourceUploadProcess() {}
    
    ResourceUploadProcess(String feederId, String fplId, String resourceId, ResourceType resourceType, boolean replace, String fileName, String contentType) {
        this.contentType = contentType;
        this.feederId = feederId;
        this.fileName = fileName;
        this.fplId = fplId;
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
                FeederWebService ssvc = env.obtainWebService(FeederWebService.class);

                ResourceSearchParams rsparams = null;
                ResourceMetadata rmeta = null;
                boolean isNew = true;
                
                if (resourceId == null) {
                    rmeta = new ResourceMetadata();
                    rmeta.setResourceId(UUID.randomUUID().toString());
                    rmeta.setType(resourceType);
                    if (replace) {
                        // Attempt to locate the resource and overwrite it if it exists.
                        rsparams = new ResourceSearchParams();
                        rsparams.setType(resourceType);
                    }
                    
                    if (fplId != null) {
                        PoleWebService psvc = env.obtainWebService(PoleWebService.class);
                        PoleSearchParams pparams = new PoleSearchParams();
                        pparams.setFPLId(fplId);
                        Pole p = CollectionsUtilities.firstItemIn(psvc.search(env.obtainAccessToken(), pparams));
                        if (p == null) {
                            System.err.printf("No pole found for FPL ID \"%s\".\n", fplId);
                            return true;
                        } else {
                            PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
                            PoleInspectionSearchParams piparams = new PoleInspectionSearchParams();
                            piparams.setPoleId(p.getId());
                            PoleInspection pi = CollectionsUtilities.firstItemIn(pisvc.search(env.obtainAccessToken(), piparams));
                            if (pi == null) {
                                System.err.printf("No inspection for pole with FPL ID \"%s\" found.\n", fplId);
                                return true;
                            } else {
                                Feeder ss = ssvc.retrieve(env.obtainAccessToken(), p.getSiteId());
                                if (rsparams != null) {
                                    rsparams.setOrganizationId(ss.getOrganizationId());
                                    rsparams.setAssetId(p.getId());
                                    rsparams.setAssetInspectionId(pi.getId());
                                    rsparams.setSiteId(ss.getId());
                                }
                                rmeta.setOrganizationId(ss.getOrganizationId());
                                rmeta.setAssetId(p.getId());
                                rmeta.setAssetInspectionId(pi.getId());
                                rmeta.setSiteId(ss.getId());
                            }
                        }
                    } else if (feederId != null) {
                        FeederSearchParams params = new FeederSearchParams();
                        params.setFeederNumber(feederId);
                        Feeder ss = CollectionsUtilities.firstItemIn(ssvc.search(env.obtainAccessToken(), params));
                        if (ss == null) {
                            System.err.printf("No substation found for feeder \"%s\".\n", feederId);
                            return true;
                        }
                        if (rsparams != null) {
                            rsparams.setOrganizationId(ss.getOrganizationId());
                            rsparams.setSiteId(ss.getId());
                        }
                    } else {
                        // Shouldn't get here due to checks above
                        System.err.println("Either Resource ID, Feeder ID or FPL ID are required.");
                        return false;
                    }
                    
                    if (rsparams != null) {
                        // Look for the resource metadata
                        rmeta = CollectionsUtilities.firstItemIn(rsvc.query(env.obtainAccessToken(), rsparams));
                        if (rmeta == null) {
                            rmeta = new ResourceMetadata();
                            rmeta.setOrganizationId(rsparams.getOrganizationId());
                            rmeta.setAssetId(rsparams.getAssetId());
                            rmeta.setAssetInspectionId(rsparams.getAssetInspectionId());
                            rmeta.setResourceId(UUID.randomUUID().toString());
                            rmeta.setSiteId(rsparams.getSiteId());
                            rmeta.setType(rsparams.getType());
                        } else {
                            isNew = false;
                        }
                    }

                    if (isNew) {
                        rmeta.setContentType(contentType);
                        rmeta.setName(f.getName());
                        rmeta.setStatus(ResourceStatus.QueuedForUpload);
                        rmeta.setTimestamp(ZonedDateTime.now());
                        rsvc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
                    }
                } else {
                    rmeta = rsvc.retrieve(env.obtainAccessToken(), resourceId);
                    if (rmeta == null) {
                        System.err.printf("No resource with ID \"%s\" found in data store.\n", resourceId);
                        return true;
                    }
                }

                HttpClientUtilities.postFile(env, rmeta.getResourceId(), contentType, f);

                if (resourceId == null && ResourceStatus.Released != rmeta.getStatus()) {
                    // If this is a new upload rather than a re-upload, switch status to Released.
                    rmeta.setStatus(ResourceStatus.Released);
                    rsvc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                }
                
                System.out.printf("The file \"%s\" of type %s has been uploaded to %s with resourceId %s\n", f, resourceType, feederId, rmeta.getResourceId());
            } catch (IOException  | URISyntaxException ex) {
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

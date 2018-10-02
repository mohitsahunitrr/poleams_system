package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Queue;
import java.util.UUID;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;

/**
 *
 * @author pchapman
 */
public class ResourceUploadProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_FEEDER_ID = "-feeder";
    private static final String ARG_FPL_ID = "-fplid";
    private static final String ARG_RESOURCE_ID = "-resourceId";
    private static final String ARG_TYPE = "-type";
    private static final String COMMAND = "uploadResource";

    private static final String HELP =
            "\t" + COMMAND
            + " [" + ARG_FEEDER_ID + " FeederId]"
            + " [" + ARG_FPL_ID + " FPL_Id]"
            + " [" + ARG_TYPE + " ResourceType]"
            + " [" + ARG_RESOURCE_ID + " ResourceId] "
            + " resourceType path/to/resource";

    private String feederId;
    private String fplId;
    private String fileName;
    private String resourceId;
    private ResourceType resourceType;
    
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
            case ARG_RESOURCE_ID:
                if (resourceId == null) {
                    resourceId = args.poll();
                    return resourceId != null;
                } else {
                    return false;
                }
            case ARG_TYPE:
                if (resourceType == null) {
                    resourceType = ResourceType.valueOf(args.poll());
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
        
        String contentType = null;
        try {
            ImageFormat format = Imaging.guessFormat(f);
            if (ImageFormats.UNKNOWN.equals(format)) {
                if (fn.endsWith(".ZIP")) {
                    contentType = "application/zip";
                } else if (fn.endsWith(".KML")) {
                    contentType = "application/vnd.google-earth.kml+xml";
                } else if (fn.endsWith(".KMZ")) {
                    contentType = "application/vnd.google-earth.kmz";
                } else if (fn.endsWith(".PDF")) {
                    contentType = "application/pdf";
                } else if (fn.endsWith(".XLSX")) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
            } else {
                ImageInfo info = Imaging.getImageInfo(f);
                ImageMetadata metadata = Imaging.getMetadata(f);
                contentType = info.getMimeType();
            }
        } catch (IOException | ImageReadException ioe) {
            ioe.printStackTrace(System.err);
            return true;
        }
        if (contentType == null) {
            System.err.printf("The file \"%s\" is an unrecognized file type.", f);
        } else {
            try {
                ResourceWebService rsvc = env.obtainWebService(ResourceWebService.class);
                SubStationWebService ssvc = env.obtainWebService(SubStationWebService.class);

                ResourceMetadata rmeta;
                SubStation ss;
                
                if (resourceId == null) {
                    rmeta = new ResourceMetadata();
                    
                    if (fplId != null) {
                        PoleWebService psvc = env.obtainWebService(PoleWebService.class);
                        PoleSearchParameters pparams = new PoleSearchParameters();
                        pparams.setFPLId(fplId);
                        Pole p = CollectionsUtilities.firstItemIn(psvc.search(env.obtainAccessToken(), pparams));
                        if (p == null) {
                            System.err.printf("No pole found for FPL ID \"%s\".\n", fplId);
                            return true;
                        } else {
                            PoleInspectionWebService pisvc = env.obtainWebService(PoleInspectionWebService.class);
                            PoleInspectionSearchParameters piparams = new PoleInspectionSearchParameters();
                            piparams.setPoleId(p.getId());
                            PoleInspection pi = CollectionsUtilities.firstItemIn(pisvc.search(env.obtainAccessToken(), piparams));
                            if (pi == null) {
                                System.err.printf("No inspection for pole with FPL ID \"%s\" found.\n", fplId);
                                return true;
                            } else {
                                rmeta.setPoleId(p.getId());
                                rmeta.setPoleInspectionId(pi.getId());
                                ss = ssvc.retrieve(env.obtainAccessToken(), p.getSubStationId());
                            }
                        }
                    } else if (feederId != null) {
                        SubStationSearchParameters params = new SubStationSearchParameters();
                        params.setFeederNumber(feederId);
                        ss = CollectionsUtilities.firstItemIn(ssvc.search(env.obtainAccessToken(), params));
                        if (ss == null) {
                            System.err.printf("No substation found for feeder \"%s\".\n", feederId);
                            return true;
                        }
                    } else {
                        // Shouldn't get here due to checks above
                        System.err.println("Either Resource ID, Feeder ID or FPL ID are required.");
                        return false;
                    }

                    rmeta.setContentType(contentType);
                    rmeta.setName(f.getName());
                    rmeta.setOrganizationId(ss.getOrganizationId());
                    rmeta.setResourceId(UUID.randomUUID().toString());
                    rmeta.setStatus(ResourceStatus.QueuedForUpload);
                    rmeta.setSubStationId(ss.getId());
                    rmeta.setTimestamp(ZonedDateTime.now());
                    rmeta.setType(resourceType);
                    rsvc.insertResourceMetadata(env.obtainAccessToken(), rmeta);
                } else {
                    rmeta = rsvc.retrieve(env.obtainAccessToken(), resourceId);
                    if (rmeta == null) {
                        System.err.printf("No resource with ID \"%s\" found in data store.\n", resourceId);
                        return true;
                    }
                }

                HttpClientUtilities.postFile(env, rmeta.getResourceId(), contentType, f);

                if (resourceId == null) {
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
        output.println("\tResource Types:");
        for (ResourceType type : ResourceType.values()) {
            output.printf("\t\t%s\n", type.name());
        }
    }
    
}

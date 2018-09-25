/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.processor.zoomify;

import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.ResourceSummary;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceStatus;
import com.precisionhawk.poleams.processor.bean.JobInfo;
import com.precisionhawk.poleams.support.httpclient.HttpClientUtilities;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.papernapkin.liana.util.StringUtil;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class ZoomifyJob {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final String UPLOAD_URL = "%s/resource/%s/upload";
    
    // Time to allow the zoomify process to run before timing out, in seconds.
    private static final long ZOOMIFY_PROCESS_TIMEOUT = 120;
    
    public ZoomifyJob() {
        LOGGER.info("Zoomify task has been instantiated");
    }
    
    private String executablePath;
    public String getExecutablePath() {
        return executablePath;
    }
    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }
    
    //TODO: This is a hack
    private JobInfo jobInfo;
    public JobInfo getJobInfo() {
        return jobInfo;
    }
    public void setJobInfo(JobInfo  jobInfo) {
        this.jobInfo = jobInfo;
    }
    
    private File tempDir;
    public void setTemporaryPath(String tempPath) {
        // I really hate putting code like this in a setter, but I need this
        // to fail on init.
        tempDir = new File(tempPath);
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                throw new IllegalArgumentException(StringUtil.replaceArgs("Unable to create the temporary directory {1}", tempPath));
            }
        }
    }
    
    private List<Environment> environments;
    public List<Environment> getEnvironments() {
        return environments;
    }
    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }
    
    public void zoomifyImages() throws JobExecutionException {
        ZonedDateTime start = ZonedDateTime.now();
        int count = 0;
        ResourceMetadata rmeta;
        int subcount;
        String zoomifyId;
        getJobInfo().update(start, null, count, "Running.");
        ResourceSearchParameters criteria = new ResourceSearchParameters();
        criteria.setStatus(ResourceStatus.Processed);
        ResourceWebService svc;
        LOGGER.info("Executing Zoomify task.  There are {} enviornments being monitored.", environments.size());
        for (Environment env : environments) {
            try {
                getJobInfo().update(start, null, count, String.format("Processing %s", env.getName()));
                subcount = 0;
                LOGGER.info("Querying for resources in enviornment {} that may need to be zoomified.", env.getName());
                List<ResourceSummary> resources;
                try {
                    svc = env.obtainWebService(ResourceWebService.class);
                    resources = svc.querySummaries(env.obtainAccessToken(), criteria);
                } catch (IOException ex) {
                    LOGGER.error("Error obtaining list of resources for environment {}", env.getName(), ex);
                    getJobInfo().update(start, ZonedDateTime.now(), count, String.format("Error obtaining list of resources for environment %s", env.getName()));
                    throw new JobExecutionException(String.format("Error getting access token for service %s", env.getName()), ex);
                }
                LOGGER.info("Found {} resources in environment {} that may need to be zoomified.", resources.size(), env.getName());
                for (ResourceSummary smry : resources) {
                    if (smry.getContentType().startsWith("image/") && smry.getZoomifyURL() == null && smry.getScaledImageURL() == null) {
                        LOGGER.info("Downloading image {} to zoomify it", smry.getResourceId());
                        File sourceFile = createSourceFile(smry);
                        File targetFile = createTargetFile(smry);
                        try {
                            // Download resource into temporary file.
                            String location = smry.getDownloadURL();
                            URL url;
                            HttpURLConnection conn = null;
                            url = new URL(location);
                            // It may redirect to AWS, so account for that
                            boolean searching = true;
                            boolean found = false;
                            while (searching) {
                                LOGGER.info("Downloading resource from {}", url);
                                conn = (HttpURLConnection)url.openConnection();
                                conn.setConnectTimeout(15000);
                                conn.setReadTimeout(15000);
                                conn.setInstanceFollowRedirects(false);
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0...");
                                switch (conn.getResponseCode())
                                {
                                    case HttpURLConnection.HTTP_OK:
                                        searching = false;
                                        found = true;
                                        break;
                                    case HttpURLConnection.HTTP_NOT_FOUND:
                                        LOGGER.error("Image for resource {} not found.", smry.getResourceId());
                                        searching = false;
                                        found = false;
                                        break;
                                    case HttpURLConnection.HTTP_MOVED_PERM:
                                    case HttpURLConnection.HTTP_MOVED_TEMP:
                                        location = conn.getHeaderField("Location");
                                        url = new URL(url, location);  // Deal with relative URLs
                                        LOGGER.info("Redirected to {}", url);
                                        break;
                                    default:
                                        LOGGER.error("Unexpected HTTP response {} downloading resource {}.", conn.getResponseCode(), smry.getResourceId());
                                        searching = false;
                                        found = false;
                                        break;
                                }
                            }
                            if (found && conn != null) {
                                InputStream is = null;
                                OutputStream os = null;
                                try {
                                    os = new BufferedOutputStream(new FileOutputStream(sourceFile));
                                    is = conn.getInputStream();
                                    IOUtils.copy(is, os);
                                } finally {
                                    IOUtils.closeQuietly(is);
                                    IOUtils.closeQuietly(os);
                                    is = null;
                                }
                                LOGGER.info("Zoomifying image {} from {} into {}", smry.getResourceId(), sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
                                ProcessBuilder pb = new ProcessBuilder(getExecutablePath(), "-Z", "-o", targetFile.getAbsolutePath(), sourceFile.getAbsolutePath());
                                Process proc = pb.start();
                                if (proc == null) {
                                    // Invalid state.  Possibly too many open file handles.
                                    LOGGER.error("ProcessBuilder returned NULL from starting zoomify process.  Aborting this run.");
                                    getJobInfo().update(start, ZonedDateTime.now(), count, "ProcessBuilder returned NULL from starting zoomify process.  Run aborted.");
                                    searching = false;
                                    return;
                                }
                                if (proc.waitFor(ZOOMIFY_PROCESS_TIMEOUT, TimeUnit.SECONDS)) {
                                    int resp = proc.exitValue();
                                    if (resp == 0) {
                                        // Prepare metadata
                                        rmeta = svc.retrieve(env.obtainAccessToken(), smry.getResourceId());
                                        if (rmeta.getZoomifyId() == null) {
                                            zoomifyId = UUID.randomUUID().toString();
                                            rmeta.setZoomifyId(zoomifyId);
                                            LOGGER.info("Storing zoomify data of image {} with resource ID {}", rmeta.getResourceId(), rmeta.getZoomifyId());
                                            svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                                        }
                                        // Upload zoomify resource
                                        String uri = String.format(UPLOAD_URL, env.getServiceURI(), rmeta.getZoomifyId());
                                        HttpClientUtilities.postFile(new URI(uri), env.obtainAccessToken(), "application/binary", targetFile);
                                        LOGGER.info("Zoomify data of image {} saved with zoonify ID {}", rmeta.getResourceId(), rmeta.getZoomifyId());
                                        rmeta.setStatus(ResourceStatus.Released);
                                        svc.updateResourceMetadata(env.obtainAccessToken(), rmeta);
                                        LOGGER.info("Status \"{}\" and zoomify ID of image {} saved", rmeta.getStatus(), rmeta.getResourceId());
                                        LOGGER.info("Zoomify of image {} completed", smry.getResourceId());
                                        sourceFile.delete();
                                        targetFile.delete();
                                        LOGGER.info("Source file {} and target file {} has been deleted", sourceFile.getAbsolutePath(), targetFile.getAbsoluteFile());
                                        count++;
                                        subcount++;
                                        getJobInfo().update(start, null, count, String.format("Processing %s", env.getName()));
                                    } else {
                                        LOGGER.error("Unable to zoomify resource {}, received return code {}. See http://www.zoomify.com/converters.htm#converterCommand for details.", smry.getResourceId());
                                        LOGGER.info("Source file {} and target file {} retained", sourceFile.getAbsolutePath(), targetFile.getAbsoluteFile());
                                    }
                                } else {
                                    // Zoomify did not exit before the timeout
                                    LOGGER.error("Zoomify was not able to process resource with ID {} within {} seconds. ABORTING", smry.getResourceId(), ZOOMIFY_PROCESS_TIMEOUT);
                                    // Clean up
                                    proc.destroyForcibly();
                                    getJobInfo().update(start, null, count, String.format("Zoomify was not able to process resource with ID %s within %s seconds. ABORTED", smry.getResourceId(), ZOOMIFY_PROCESS_TIMEOUT));
                                    return;
                                }
                            }
                        } catch (Throwable t) {
                            LOGGER.error("Unable to zoomify resource {}", smry.getResourceId(), t);
                            LOGGER.info("Source file {} and target file {} retained, if they where created", sourceFile.getAbsolutePath(), targetFile.getAbsoluteFile());
                        } finally {
        //                    if (sourceFile != null && sourceFile.exists()) {
        //                        sourceFile.delete();
        //                    }
        //                    if (targetFile != null && targetFile.exists()) {
        //                        targetFile.delete();
        //                    }
                        }
                    } else {
                        LOGGER.info("Resource {} is not an image.", smry.getResourceId());
                    }
                }
                LOGGER.info("{} resources zoomified in environment {}.", subcount, env.getName());
            } catch (Throwable t) {
                LOGGER.error("Error zoomifying images in environment {}.", env.getName(), t);
            }
        }
        getJobInfo().update(start, ZonedDateTime.now(), count, "Last run successful");
        
        LOGGER.info("Finished job {}", jobInfo.getJobName());
    }
    
    private File createSourceFile(ResourceSummary res) {
        String ext = null;
        if (null != res.getContentType()) switch (res.getContentType()) {
            case "image/jpeg":
                ext = ".jpeg";
                break;
            case "image/gif":
                ext = ".gif";
                break;
            case "image/png":
                ext = ".png";
                break;
        }
        String name = res.getResourceId();
        if (ext != null) {
            name = name + ext;
        }
        return new File(tempDir, name);
    }
    
    private File createTargetFile(ResourceSummary res) {
        return new File(tempDir, res.getResourceId() + ".zif");
    }
}

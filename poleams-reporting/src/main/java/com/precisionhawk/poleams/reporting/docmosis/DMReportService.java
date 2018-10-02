package com.precisionhawk.poleams.reporting.docmosis;

import com.precisionhawk.poleams.reporting.ReportService;
import com.precisionhawk.poleams.reporting.ReportingConfig;
import com.precisionhawk.poleams.reporting.ReportingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pchapman
 */
public class DMReportService implements ReportService {
    
private static final CloseableHttpClient CLIENT = HttpClients.createDefault();
private static final URI DWS_RENDER_URL;
    static {
        try {
            DWS_RENDER_URL = new URI("https://dws2.docmosis.com/services/rs/render");
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid URI", ex);
        }
    }
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    private ReportingConfig config;
    @Override
    public void setConfig(ReportingConfig config) {
        this.config = config;
    }

    @Override
    public InputStream generateReport(String reportId, String dataJSON, String outFileName)
        throws ReportingException
    {
        HttpPost req = new HttpPost(DWS_RENDER_URL);

        // build request
        StringBuilder sb = new StringBuilder();

        // Start building the instruction
        sb.append("{\n");
        sb.append("\"accessKey\":\"").append(config.getAccessKey()).append("\",\n");
        sb.append("\"templateName\":\"").append(reportId).append("\",\n");
        sb.append("\"outputName\":\"").append(outFileName).append("\",\n");
        // now add the data specifically for this template
        sb.append("\"data\":");
        sb.append(dataJSON);
        sb.append("}\n");

        LOGGER.debug("Sending request: {}", sb.toString().replace(config.getAccessKey(), "****"));
        
        OutputStream os = null;
        try {
            HttpEntity entity = new StringEntity(sb.toString(), ContentType.create("application/json; charset=utf-8"));
            req.setEntity(entity);
            HttpResponse resp = CLIENT.execute(req);

            int status = resp.getStatusLine().getStatusCode();
            if (status == 200) {
                // successful render,
                // save our document to temporary file
                File file = File.createTempFile(reportId, outFileName);
                os = new FileOutputStream(file);
                IOUtils.copy(resp.getEntity().getContent(), os);

                LOGGER.debug("Created file %s with contents for report %s", file.getAbsolutePath(), reportId);
                
                return new FileInputStream(file);
            } else {
                throw ReportingException.build(null, "Unsuccessful executing report %s, received status %d with message \"%s\"", reportId, status, resp.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            throw ReportingException.build(ex, "Error executing report %s", reportId);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }
    
}

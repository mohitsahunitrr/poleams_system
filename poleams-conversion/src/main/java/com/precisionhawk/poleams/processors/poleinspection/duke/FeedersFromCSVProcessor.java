package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author pchapman
 */
 // Developed for Duke import
public class FeedersFromCSVProcessor {
    public static InspectionData process(Environment env, ProcessListener listener, File feederCSV, String orgId) {
        listener.reportMessage(String.format("Processing CSV file %s", feederCSV.getName()));
        WSClientHelper svcs = new WSClientHelper(env);
        InspectionData data = new InspectionData();
        data.setOrganizationId(orgId);
        Reader in = null;
        Feeder feeder;
        String feederName;
        String feederNumber;
        String priority;
        FeederSearchParams params = new FeederSearchParams();
        params.setOrganizationId(orgId);
        FeederWebService svc = env.obtainWebService(FeederWebService.class);
        try {
            in = new FileReader(feederCSV);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord record : records) {
                if (record.getRecordNumber() < 3) {
                    // Skip
                } else {
                    feederNumber = record.get(3);
                    feederName = record.get(0);
                    priority = record.get(2);
                    if (!"1".equals(priority)) {
                        continue;
                    }
                    if (feederName.isEmpty() && feederNumber.isEmpty()) {
                        break;
                    }
                    DataImportUtilities.ensureFeeder(svcs, data, listener, feederNumber, feederName, "Pending");
                }
            }
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return data;
    }
}

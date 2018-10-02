package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.poleams.reporting.ReportService;
import com.precisionhawk.poleams.reporting.ReportingConfig;
import com.precisionhawk.poleams.reporting.ReportingException;
import com.precisionhawk.poleams.reporting.docmosis.DMReportService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Queue;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author pchapman
 */
public class GenerateReportProcess extends CommandProcess {
    
    private static final String ARG_API_KEY = "-k";
    private static final String ARG_INFILE = "-i";
    private static final String ARG_OUTFILE = "-o";
    private static final String ARG_RPT_ID = "-r";
    private static final String COMMAND = "generateReport";
    private static final String HELP =
            "\t" + COMMAND
            + " " + ARG_API_KEY + " Docmosis_API_Key"
            + " " + ARG_RPT_ID + " ReportId"
            + " " + ARG_INFILE + " /data/input/file "
            + " " + ARG_OUTFILE + " /path/to/write/result";

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {
        output.println(HELP);
    }

    @Override
    public boolean process(Queue<String> args) {
        String apikey = null;
        String infile = null;
        String outfile = null;
        String reportId = null;
        
        // Process arguments
        for (String arg = args.poll(); arg != null; arg = args.poll()) {
            switch (arg) {
                case ARG_API_KEY:
                    if (apikey != null) {
                        return false;
                    }
                    apikey = args.poll();
                    if (apikey == null) {
                        return false;
                    }
                case ARG_INFILE:
                    if (infile != null) {
                        return false;
                    }
                    infile = args.poll();
                    if (infile == null) {
                        return false;
                    }
                    break;
                case ARG_OUTFILE:
                    if (outfile != null) {
                        return false;
                    }
                    outfile = args.poll();
                    if (outfile == null) {
                        return false;
                    }
                    break;
                case ARG_RPT_ID:
                    if (reportId != null) {
                        return false;
                    }
                    reportId = args.poll();
                    if (reportId == null) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }

        // Test for necessary parameters
        if (
                apikey == null || apikey.isEmpty()
                || infile == null || infile.isEmpty()
                || outfile == null || outfile.isEmpty()
                || reportId == null || reportId.isEmpty()
            )
        {
            return false;
        }
        
        // Read data
        String json = null;
        File inf = new File(infile);
        if (!inf.canRead()) {
            System.err.printf("Unable to read input file %s\\n", infile);
            return true;
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(inf);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            os = baos;
            IOUtils.copy(is, baos);
            baos.flush();
            json = new String(baos.toByteArray(), "UTF-8"); // We assume UTF-8
        } catch (IOException ex) {
            System.err.printf("Unable to read input file %s\\n", infile);
            ex.printStackTrace(System.err);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
            is = null;
            os = null;
        }
        File outf = new File(outfile);
        final String key = apikey;
        try {
            ReportService svc = new DMReportService();
            svc.setConfig(new ReportingConfig() {
                public String getAccessKey() {
                    return key;
                }
            });
            is = svc.generateReport(reportId, json, outf.getName());
            os = new FileOutputStream(outf);
            IOUtils.copy(is, os);
            System.out.printf("The results have been written out to %s", infile);
        } catch (IOException | ReportingException ex) {
            System.err.println("Unable to generate the report.");
            ex.printStackTrace(System.err);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
        return true;
    }
    
}

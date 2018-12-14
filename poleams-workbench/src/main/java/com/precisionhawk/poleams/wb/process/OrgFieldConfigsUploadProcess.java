package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.orgconfig.OrgFieldTranslations;
import com.precisionhawk.ams.bean.orgconfig.OrgFieldValidations;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.OrganizationWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author pchapman
 */
public class OrgFieldConfigsUploadProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_TRANS_PATH = "-t";
    private static final String ARG_VAL_PATH = "-v";
    private static final String COMMAND = "uploadOrgFieldConfigs";
    private static final String HELP = "\t" + COMMAND
            + " [ " + ARG_TRANS_PATH + " /path/translations.json]"
            + " [ " + ARG_VAL_PATH + " /path/translations.json]"
            ;

    private String transPath;
    private String valPath;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {

        switch (arg) {
            case ARG_TRANS_PATH:
                if (transPath != null) {
                    return false;
                }
                transPath = args.poll();
                return transPath != null;
            case ARG_VAL_PATH:
                if (valPath != null) {
                    return false;
                }
                valPath = args.poll();
                return valPath != null;
            default:
                return false;
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (transPath == null && valPath == null) {
            return false;
        }
        if (transPath != null) {
            OrgFieldTranslations trans = null;
            try {
                ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
                trans = mapper.readValue(new File(transPath), OrgFieldTranslations.class);
                env.obtainWebService(OrganizationWebService.class).postOrgTranslations(env.obtainAccessToken(), trans);
            } catch (IOException ex) {
                System.err.println("Error updating translations JSON.");
                ex.printStackTrace(System.err);
            }
        }
        if (valPath != null) {
            OrgFieldValidations vals = null;
            try {
                ObjectMapper mapper = new ObjectMapperFactory().get();
                vals = mapper.readValue(new File(valPath), OrgFieldValidations.class);
                env.obtainWebService(OrganizationWebService.class).postOrgFieldValidations(env.obtainAccessToken(), vals);
            } catch (IOException ex) {
                System.err.println("Error updating validations JSON.");
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
    }    
}

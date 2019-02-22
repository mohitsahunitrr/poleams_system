package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.orgconfig.OrgFieldTranslations;
import com.precisionhawk.ams.domain.Organization;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.OrganizationWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Queue;
import java.util.UUID;

/**
 *
 * @author pchapman
 */
public class CreateOrganizationProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DEFAULT_TRANS = "-dt";
    private static final String ARG_ORG_ID = "-id";
    private static final String COMMAND = "createOrganization";
    
    private static final String HELP = COMMAND + " [" + ARG_DEFAULT_TRANS + "] [" + ARG_ORG_ID  + " organizationId] OrganizationName\n\t-dt Copy the default translations JSON for this org.";

    private boolean copyDefaultTrans = false;
    private String id = null;
    private String orgName = null;
    
    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        switch (arg) {
            case ARG_DEFAULT_TRANS:
                if (copyDefaultTrans) {
                    // Already been set once.
                    return false;
                }
                copyDefaultTrans = true;
                return true;
            case ARG_ORG_ID:
                if (id == null) {
                    id = args.poll();
                    return id != null;
                } else {
                    // Already been set once.
                    return false;
                }
            default:
                if (orgName == null) {
                    orgName = arg;
                    return orgName != null;
                } else {
                    // Already been set once.
                    return false;
                }
        }
    }

    @Override
    protected boolean execute(Environment env) {
        if (orgName == null) {
            return false;
        }
        Organization ph = null;
        OrganizationWebService svc = env.obtainWebService(OrganizationWebService.class);
        // Ensure no org with the given name exists.
        for (Organization org : svc.retrieveOrgs()) {
            if (org.getName().equals(orgName)) {
                System.err.printf("Organization \"%s\" with the name \"%s\" already exists.  Aborting.", org.getId(), orgName);
                return true;
            } else if (org.getKey().equals(OrganizationWebService.COMPANY_ORG_KEY)) {
                ph = org;
            }
        }
        if (copyDefaultTrans && ph == null) {
            // Shouldn't happen
            System.err.println("Unable to locate the PrecisionHawk organization.  Aborting.");
            return true;
        }
        // The org doesn't exist.  Create it.
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Organization org = new Organization();
        org.setId(id);
        org.setKey(orgName.replaceAll(" ", ""));
        org.setName(orgName);
        try {
            svc.createOrg(env.obtainAccessToken(), org);
            System.out.printf("Created organization \"%s\" with ID \"%s\".\n", org.getName(), org.getId());
            
            if (copyDefaultTrans) {
                OrgFieldTranslations trans = svc.retrieveOrgTranslations(ph.getId(), null, null);
                trans.setId(UUID.randomUUID().toString());
                trans.setOrganizationId(id);
                trans.setUpdated(LocalDate.now());
                svc.postOrgTranslations(env.obtainAccessToken(), trans);
                System.out.printf("Copied default translations.json with new ID \"%s\".\n", trans.getId());
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
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

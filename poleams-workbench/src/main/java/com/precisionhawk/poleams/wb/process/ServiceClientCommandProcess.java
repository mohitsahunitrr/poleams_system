package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.Organization;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.webservices.OrganizationWebService;
import com.precisionhawk.ams.webservices.WorkOrderWebService;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.util.PAComparators;
import com.precisionhawk.poleams.webservices.FeederWebService;
import java.io.Console;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Adds some functionality to com.precisionhawk.ams.wb.process.ServiceClientCommandProcess.
 *
 * @author pchapman
 */
public abstract class ServiceClientCommandProcess extends com.precisionhawk.ams.wb.process.ServiceClientCommandProcess {
    
    //TODO: All these members are very similar and their code can probably be refactored given a object formatter and prompt text.
    //FIXME: The below methods do not test input number against bounds of array.
    
    @SuppressWarnings({"rawtypes","unchecked"})
    protected Feeder chooseFeeder(Environment env, Console c, String orgId) throws IOException {
        FeederSearchParams params = new FeederSearchParams();
        params.setOrganizationId(orgId);
        List<Feeder> feeders = env.obtainWebService(FeederWebService.class).search(env.obtainAccessToken(), params);
        List l = feeders;
        Collections.sort(l, PAComparators.FEEDERS_COMPARATOR); // Trick java into sorting Feeders as the super
        Feeder f;
        c.printf("\n");
        for (int i = 0; i < feeders.size(); i++) {
            f = feeders.get(i);
            c.printf("%d\t%s\t%s\n", i + 1, f.getId(), f.getName());
        }
        f = null;
        String s;
        do {
            s = c.readLine("Choose a feeder (or X to exit):\n");
            if ("X".equalsIgnoreCase(s)) {
                return null;
            }
            try {
                int i = Integer.valueOf(s);
                f = feeders.get(i - 1);
                c.printf("\nSite %s selected\n", f.getName());
            } catch (NumberFormatException ex) {
                c.printf("Invalid option %s\n\n", s);
            }
        } while (f == null);
        return f;
    }
    
    protected Organization chooseOrganization(Environment env, Console c) throws IOException {
        List<Organization> orgs = env.obtainWebService(OrganizationWebService.class).retrieveOrgs();
        Collections.sort(orgs, PAComparators.ORGS_COMPARATOR);
        Organization org;
        c.printf("\n");
        for (int i = 0; i < orgs.size(); i++) {
            org = orgs.get(i);
            c.printf("%d\t%s\t%s\n", i + 1, org.getId(), org.getName());
        }
        org = null;
        String s;
        do {
            s = c.readLine("Choose an organization (or X to exit):\n");
            if ("X".equalsIgnoreCase(s)) {
                return null;
            }
            try {
                int i = Integer.valueOf(s);
                org = orgs.get(i - 1);
                c.printf("\nOrganization %s selected\n", org.getName());
            } catch (NumberFormatException ex) {
                c.printf("Invalid option %s\n\n", s);
            }
        } while (org == null);
        return org;
    }
    
    protected WorkOrder chooseWorkOrder(Environment env, Console c, Feeder f) throws IOException {
        WorkOrderSearchParams params = new WorkOrderSearchParams();
        params.setSiteId(f.getId());
        List<WorkOrder> orders = env.obtainWebService(WorkOrderWebService.class).search(env.obtainAccessToken(), params);
        Collections.sort(orders, PAComparators.WORK_ORDERS_COMPARATOR);
        WorkOrder wo;
        c.printf("\n");
        for (int i = 0; i < orders.size(); i++) {
            wo = orders.get(i);
            c.printf("%d\t%s\n", i + 1, wo.getOrderNumber());
        }
        wo = null;
        String s;
        do {
            s = c.readLine("Choose a work order (or X to exit):\n");
            if ("X".equalsIgnoreCase(s)) {
                return null;
            }
            try {
                int i = Integer.valueOf(s);
                wo = orders.get(i - 1);
                c.printf("\nWork order %s selected\n", wo.getOrderNumber());
            } catch (NumberFormatException ex) {
                c.printf("Invalid option %s\n\n", s);
            }
        } while (wo == null);
        return wo;
    }
    
}

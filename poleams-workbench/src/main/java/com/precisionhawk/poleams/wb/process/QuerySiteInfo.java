package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.Organization;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author pchapman
 */
public class QuerySiteInfo extends ServiceClientCommandProcess {
    
    private static final String COMMAND = "querySiteInfo";
    
    private static final Comparator<Organization> ORG_COMPARATOR = new Comparator<Organization>() {
        @Override
        public int compare(Organization o1, Organization o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    private static final Comparator<Feeder> FEEDER_COMPARATOR = new Comparator<Feeder>() {
        @Override
        public int compare(Feeder f1, Feeder f2) {
            return f1.getName().compareTo(f2.getName());
        }
    };
    
    private static final Comparator<WorkOrder> WORK_ORDER_COMPARATOR = new Comparator<WorkOrder>() {
        @Override
        public int compare(WorkOrder wo1, WorkOrder wo2) {
            return wo1.getOrderNumber().compareTo(wo2.getOrderNumber());
        }
    };

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper svcs = new WSClientHelper(env);
        Console c = System.console();
        String  s;
        
        try {
            // Choose organization
            List<Organization> orgs = svcs.organizations().retrieveOrgs();
            Collections.sort(orgs, ORG_COMPARATOR);
            
            for (int i = 0; i < orgs.size(); i++) {
                c.printf("%d\t%s\t%s\n", i, orgs.get(i).getId(), orgs.get(i).getName());
            }
            int i = -1;
            do {
                s = c.readLine("Chose an organization or X/x to exit.\t");
                if ("x".equals(s.toLowerCase())) {
                    return true;
                } else {
                    try {
                        i = Integer.valueOf(s);
                        if (i < 0 || i >= orgs.size()) {
                            i = -1;
                        }
                    } catch (NumberFormatException nfe) {
                        // Do nothing
                    }
                }
            } while (i == -1);
            
            FeederSearchParams fsparams = new FeederSearchParams();
            fsparams.setOrganizationId(orgs.get(i).getId());
            List<Feeder> feeders = svcs.feeders().search(svcs.token(), fsparams);
            Collections.sort(feeders, FEEDER_COMPARATOR);
            
            WorkOrderSearchParams woparams = new WorkOrderSearchParams();
            List<WorkOrder> workOrders;
            
            for (Feeder f : feeders) {
                c.printf("%s\t%s-%s\n", f.getId(), f.getName(), f.getFeederNumber());
                woparams.setSiteId(f.getId());
                workOrders = svcs.workOrders().search(svcs.token(), woparams);
                Collections.sort(workOrders, WORK_ORDER_COMPARATOR);
                for (WorkOrder wo : workOrders) {
                    c.printf("\t%s\n", wo.getOrderNumber());
                }
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
        output.println(COMMAND + ARGS_FOR_HELP);
    }
}

package com.precisionhawk.poleams.wb.process;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.AssetInspectionType;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.SiteInspectionType;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.domain.WorkOrderType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.wb.process.ServiceClientCommandProcess;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Queue;
import java.util.UUID;

/**
 *
 * @author pchapman
 */
public class FixDataProcess extends ServiceClientCommandProcess {
    
    private static final String ARG_DRY = "-d";
    private static final String COMMAND = "fixData";
    
    // Fix inspection data for Line 10
    private boolean dry = false;

    @Override
    protected boolean processArg(String arg, Queue<String> args) {
        if (ARG_DRY.equals(arg)) {
            if (dry) {
                // Only pass the arg once
                return false;
            } else {
                dry = true;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean execute(Environment env) {
        WSClientHelper svcs = new WSClientHelper(env);
        try {
            WorkOrder wo = svcs.workOrders().retrieveById(svcs.token(), "9C44C721");
            if (!wo.getType().equals(WorkOrderTypes.DistributionLineInspection)) {
                wo.setType(WorkOrderTypes.DistributionLineInspection);
                svcs.workOrders().update(svcs.token(), wo);
            }
            SiteInspectionSearchParams siparams = new SiteInspectionSearchParams();
            siparams.setSiteId("908d7dae-5c67-42f0-9cc5-08137205a566");
            FeederInspection sinsp = CollectionsUtilities.firstItemIn(svcs.feederInspections().search(svcs.token(), siparams));
            if (sinsp == null) {
                sinsp = new FeederInspection();
                sinsp.setDateOfInspection(LocalDate.now());
                sinsp.setId(UUID.randomUUID().toString());
                sinsp.setOrderNumber(wo.getOrderNumber());
                sinsp.setSiteId(siparams.getSiteId());
                sinsp.setStatus(new SiteInspectionStatus("Processed"));
                sinsp.setType(new SiteInspectionType(WorkOrderTypes.DistributionLineInspection.getValue()));
                sinsp.setVegitationEncroachmentGoogleEarthURL("https://drive.google.com/open?id=19GtK3NykNxcE9KdAiE_aTXoXPkm5o-Ij&usp=sharing");
                svcs.feederInspections().create(svcs.token(), sinsp);
            }
            AssetInspectionType bad = new AssetInspectionType("Pole Inspection");
            AssetInspectionType good = new AssetInspectionType("DistributionLineInspection");
            AssetInspectionSearchParams params = new AssetInspectionSearchParams();
            params.setSiteId("908d7dae-5c67-42f0-9cc5-08137205a566");
            boolean update;
            for (PoleInspection insp : svcs.poleInspections().search(svcs.token(), params)) {
                update = false;
                if (insp.getSiteInspectionId() == null) {
                    insp.setSiteInspectionId(sinsp.getId());
                    update = true;
                }
                if (!good.equals(insp.getType())) {
                    insp.setType(good);
                    update = true;
                }
                if (update) {
                    svcs.poleInspections().update(svcs.token(), insp);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return true;
    }
    
    private File findImageFile(File parent, String name) {
        if (parent.isDirectory()) {
            for (File f : parent.listFiles()) {
                parent = findImageFile(f, name);
                if (parent != null) {
                    return parent;
                }
            }
            return null;
        } else if (parent.getName().equals(name)) {
            return parent;
        } else {
            return null;
        }
    }

    @Override
    public boolean canProcess(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public void printHelp(PrintStream output) {}
}

package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.poleams.support.geojson.GeoJsonFeatureSet;
import com.precisionhawk.poleams.support.geojson.GeoJsonFeature;
import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.ams.support.jackson.ObjectMapperFactory;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * We assume everything in the site that has images has been captured and processed.
 * Those with entries in the delta GeoJson have deltas that need to be recorded in
 * Duke's "Small World" software (Status "PendingMerge").  Else, they are processed
 * (Status "Complete")
 *
 * @author pchapman
 */
 // Developed for Duke import
public class GeoJsonDeltaImport implements Constants {
    
    private static final String FIELD_POLENUM = "POLE_NUMBE";

    public boolean process(Environment env, ProcessListener listener, File poleDataJson, String siteId, String orderNum) {
        boolean success = true;
        
        WSClientHelper svcs = new WSClientHelper(env);
        // Load the data from the GeoJson
        GeoJsonFeatureSet delta = null;
        ObjectMapper mapper = ObjectMapperFactory.getObjectMapper();
        try {
            delta = mapper.readValue(poleDataJson, GeoJsonFeatureSet.class);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            return false;
        }
        
        String serialNumber;
        HashMap<String, GeoJsonFeature> featuresMap = new HashMap();
        for (GeoJsonFeature feature : delta.getFeatures()) {
            serialNumber = feature.getProperties().get(FIELD_POLENUM);
            if (serialNumber == null) {
                listener.reportNonFatalError("Missing pole serial number");
            } else {
                featuresMap.put(serialNumber, feature);
            }
        }

        // Apply deltas
        InspectionData data = new InspectionData();
        data.setCurrentOrderNumber(orderNum);
        try {
            WorkOrder wo = svcs.workOrders().retrieveById(svcs.token(), orderNum);
            if (wo == null) {
                listener.reportFatalError(String.format("Work order %s not found", orderNum));
                return false;
            }
            data.setCurrentWorkOrder(wo);
            
            Feeder feeder = svcs.feeders().retrieve(svcs.token(), siteId);
            if (wo == null) {
                listener.reportFatalError(String.format("Feeder %s not found", siteId));
                return false;
            }
            data.setCurrentFeeder(feeder);
            
            PoleSearchParams pparams = new PoleSearchParams();
            pparams.setSiteId(siteId);
            for (Pole pole : svcs.poles().search(svcs.token(), pparams)) {
                AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                params.setOrderNumber(data.getCurrentOrderNumber());
                params.setAssetId(pole.getId());
                for (PoleInspection insp : svcs.poleInspections().search(svcs.token(), params)) {
                    if (insp.getSiteId() == null) {
                        insp.setSiteId(pole.getSiteId());
                    }
                    if (AI_PROCESSED.equals(insp.getStatus())) {
                        if (featuresMap.containsKey(pole.getSerialNumber())) {
                            insp.setStatus(AI_PENDING_MERGE);
                        } else {
                            insp.setStatus(AI_COMPLETE);
                        }
                        data.addPoleInspection(pole, insp, false);
                    }
                    //TODO: merge other details
                }
            }
            
            SiteInspectionSearchParams params = new SiteInspectionSearchParams();
            params.setOrderNumber(orderNum);
            params.setSiteId(siteId);
            for (FeederInspection insp : svcs.feederInspections().search(svcs.token(), params)) {
                insp.setStatus(InspectionStatuses.SI_COMPLETE);
                data.addFeederInspection(insp, false);
            }
            
            data.setCurrentFeeder(null);
            data.setCurrentOrderNumber(null);
            data.setCurrentWorkOrder(null);
            
            DataImportUtilities.saveData(svcs, listener, data);
        } catch (IOException ex) {
            listener.reportFatalException(ex);
            return false;
        }
        
        return success;
    }
}

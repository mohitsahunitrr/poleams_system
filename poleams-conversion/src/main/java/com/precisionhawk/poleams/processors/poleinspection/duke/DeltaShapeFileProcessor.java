package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.bean.ComponentSearchParams;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.ams.domain.ComponentInspection;
import com.precisionhawk.ams.domain.ComponentInspectionStatus;
import com.precisionhawk.ams.domain.ComponentInspectionType;
import com.precisionhawk.ams.domain.ComponentType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ShapeFileProcessor;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.papernapkin.liana.util.StringUtil;

/**
 *
 * @author pchapman
 */
public class DeltaShapeFileProcessor extends ShapeFileProcessor implements ShapeFileConstants {
    
    private static final String PROP_ATTR = "Attribute%d";
    private static final String PROP_COMP = "Component";
    private static final String PROP_ORIG = "Original%d";
    private static final String PROP_UPDT = "Updated%d";
    private static final String PROP_X = "X";
    private static final String PROP_Y = "Y";
    
    static final String[] PROP_ID = {"ID","OBJECT_ID","EGISID"};

    public DeltaShapeFileProcessor(WSClientHelper svcs, ProcessListener listener, InspectionData data, File shapeFile) {
        super(svcs, listener, data, shapeFile);
        if (data.getCurrentWorkOrder() == null) {
            throw new IllegalStateException("Work order required.");
        }
    }

    @Override
    protected void processFeature(Map<String, Object> featureProps) {
        String compType = StringUtil.nullableToString(featureProps.get(PROP_COMP));
        String poleSerial = StringUtil.nullableToString(featureProps.get(PROP_POLE_NUM));
        String x = StringUtil.nullableToString(featureProps.get(PROP_X));
        String y = StringUtil.nullableToString(featureProps.get(PROP_Y));
        
        if (poleSerial == null || poleSerial.isEmpty()) {
            listener.reportMessage(String.format("Pole number missing for %s at X: %s Y: %s", compType == null || compType.isEmpty() ? "pole" : compType , x, y));
            return;
        }
        
        try {
            Pole pole = null;
            for (String feederId : data.getCurrentWorkOrder().getSiteIds()) {
                pole = data.getPolesMap().get(new SiteAssetKey(feederId, poleSerial));
                if (pole != null) {
                    break;
                }
            }
            
            if (pole == null) {
                PoleSearchParams pparams = new PoleSearchParams();
                pparams.setSerialNumber(poleSerial);
                for (String feederId : data.getCurrentWorkOrder().getSiteIds()) {
                    pparams.setSiteId(feederId);
                    pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
                    if (pole != null) {
                        break;
                    }
                }
                if (pole == null) {
                    listener.reportNonFatalError(String.format("Unable to locate pole %s for work order %s", poleSerial, data.getCurrentWorkOrder().getOrderNumber()));
                    return;
                }
                data.addPole(pole, false);
            }
            
            PoleInspection pi = data.getPoleInspectionsMap().get(new SiteAssetKey(pole));
            if (pi == null) {
                AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
                aiparams.setAssetId(pole.getId());
                aiparams.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                pi = CollectionsUtilities.firstItemIn(svcs.poleInspections().search(svcs.token(), aiparams));
                if (pi == null) {
                    listener.reportFatalError(String.format("Unable to find an inspection for pole %s", pole.getId()));
                    return;
                }
                data.addPoleInspection(pole, pi, false);
            }
            
            ComponentType componentType = ShapeFilesMasterDataImport.typeOf(compType);
            if (componentType == null) {
                // Assume differences for pole
                populateAttributesDelta(pole.getAttributes(), pi.getAttributes(), featureProps);
            } else {
                // We're dealing with a component
                ComponentSearchParams cparams = new ComponentSearchParams();
                cparams.setAssetId(pole.getId());
                cparams.setType(componentType);
                if (!cparams.hasCriteria()) {
                    listener.reportNonFatalError(String.format("Unable to locate pole %s for work order %s", poleSerial, data.getCurrentWorkOrder().getOrderNumber()));
                    return;
                }
                List<Component> comps = svcs.components().query(svcs.token(), cparams);
                Component comp;
                switch (comps.size()) {
                    case 0:
                        listener.reportNonFatalError(
                            String.format("Unable to locate component %s for pole %s for work order %s", compType, poleSerial, data.getCurrentWorkOrder().getOrderNumber())
                        );
                        return;
                    case 1:
                        comp = comps.get(0);
                        data.addComponent(comp, false);
                        break;
                    default:
                        listener.reportMessage(
                            String.format("Multiple matches for %s for pole %s for work order %s", compType, poleSerial, data.getCurrentWorkOrder().getOrderNumber())
                        );
                        return;
                }
                ComponentInspectionSearchParams ciparams = new ComponentInspectionSearchParams();
                ciparams.setComponentId(comp.getId());
                ciparams.setOrderNumber(data.getCurrentWorkOrder().getOrderNumber());
                ComponentInspection ci = CollectionsUtilities.firstItemIn(svcs.componentInspections().search(svcs.token(), ciparams));
                if (ci == null) {
                    ci = createComponentInspection(pi, comp);
                }
                populateAttributesDelta(comp.getAttributes(), ci.getAttributes(), featureProps);
            }
            
        } catch (IOException ex) {
            listener.reportNonFatalException(String.format("Error parsing delta data in file %s", shapeFile), ex);
        }
    }
    
    private ComponentInspection createComponentInspection(PoleInspection pinsp, Component comp) {
        ComponentInspection insp = new ComponentInspection();
        insp.setAssetId(pinsp.getAssetId());
        insp.setAssetInspectionId(pinsp.getId());
        insp.setComponentId(comp.getId());
        insp.setId(UUID.randomUUID().toString());
        insp.setOrderNumber(pinsp.getOrderNumber());
        insp.setSiteId(pinsp.getSiteId());
        insp.setSiteInspectionId(pinsp.getSiteInspectionId());
        insp.setStatus(new ComponentInspectionStatus(pinsp.getStatus().getValue()));
        insp.setType(new ComponentInspectionType(pinsp.getType().getValue()));
        data.addComponent(comp, true);
        return insp;
    }
    
    private void populateAttributesDelta(Map<String, String> originalAttrs, Map<String, String> inspectionAttrs, Map<String, Object> featureProps) {
        String attrName;
        String attrOldVal;
        String attrNewVal;
        for (int i = 1; i < 5; i++) {
            attrName = StringUtil.nullableToString(featureProps.get(String.format(PROP_ATTR, i)));
            attrOldVal = StringUtil.nullableToString(featureProps.get(String.format(PROP_ORIG, i)));
            attrNewVal = StringUtil.nullableToString(featureProps.get(String.format(PROP_UPDT, i)));
            
            if (StringUtil.notNullNotEmpty(attrName)) {
                if (!originalAttrs.containsKey(attrName)) {
                    //TODO:
                } else {
                    inspectionAttrs.put(attrName, attrNewVal);
                }
            }
        }
    }
}

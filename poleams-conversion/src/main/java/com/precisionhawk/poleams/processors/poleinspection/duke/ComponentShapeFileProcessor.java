package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.bean.ComponentSearchParams;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.ams.domain.ComponentType;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ShapeFileProcessor;
import com.precisionhawk.poleams.processors.SiteAssetKey;
import static com.precisionhawk.poleams.processors.poleinspection.duke.ShapeFileConstants.PROPS_TO_REMOVE;
import static com.precisionhawk.poleams.processors.poleinspection.duke.ShapeFileConstants.PROP_POLE_ID;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.papernapkin.liana.util.StringUtil;

/**
 *
 * @author pchapman
 */
public class ComponentShapeFileProcessor extends ShapeFileProcessor implements ShapeFileConstants {
    
    private static final Collection<String> KEYS_TO_REMOVE = new LinkedList();
    static final String[] PROP_ID = {"ID","OBJECT_ID","EGISID"};
    static final String PROP_MODEL = "MODEL";
    static {
        KEYS_TO_REMOVE.addAll(Arrays.asList(PROPS_TO_REMOVE));
        KEYS_TO_REMOVE.add(PROP_MODEL);
    }
    
    private ComponentType componentType;

    public ComponentShapeFileProcessor(WSClientHelper svcs, ProcessListener listener, InspectionData data, File shapeFile, ComponentType componentType) {
        super(svcs, listener, data, shapeFile);
        this.componentType = componentType;
    }

    @Override
    protected void processFeature(Map<String, Object> featureProps) {
        String compId = StringUtil.nullableToString(firstOf(featureProps, PROP_ID));
        String feederNumber = StringUtil.nullableToString(featureProps.get(PROP_NETWORK_ID));
        String poleId = StringUtil.nullableToString(firstOf(featureProps, PROP_POLE_ID));
        
        if (feederNumber == null || feederNumber.isEmpty()) {
            listener.reportMessage(String.format("%s field missing for %s %s", PROP_NETWORK_ID, componentType, compId));
        }
        if (poleId == null || poleId.isEmpty()) {
            listener.reportMessage(String.format("Pole ID missing for %s %s", componentType, compId));
        }
        
        try {
            Feeder feeder = data.getFeedersByFeederNum().get(feederNumber);
            if (feeder == null) {
                FeederSearchParams fparams = new FeederSearchParams();
                fparams.setFeederNumber(feederNumber);
                feeder = CollectionsUtilities.firstItemIn(svcs.feeders().search(svcs.token(), fparams));
                if (feeder == null) {
                    listener.reportNonFatalError(String.format("Unable to locate feeder %s", feederNumber));
                    return;
                } else {
                    data.addFeeder(feeder, false);
                }
            }
            
            Pole pole = data.getPolesMap().get(new SiteAssetKey(feeder.getId(), poleId));
            if (pole == null) {
                PoleSearchParams pparams = new PoleSearchParams();
                pparams.setSiteId(feeder.getId());
                pparams.setUtilityId(poleId);
                pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
                if (pole == null) {
                    listener.reportNonFatalError(String.format("Unable to locate pole %s for feeder %s", poleId, feederNumber));
                    return;
                }
                data.addPole(pole, false);
            }
            
            // If compId = null, we trust that there is only one component of the given type for any given pole.
            ComponentSearchParams cparams = new ComponentSearchParams();
            cparams.setAssetId(pole.getId());
            cparams.setType(componentType);
            cparams.setUtilityId(compId);
            List<Component> comps = svcs.components().query(svcs.token(), cparams);
            Component comp;
            switch (comps.size()) {
                case 0:
                    comp = new Component();
                    comp.setAssetId(pole.getId());
                    comp.setId(UUID.randomUUID().toString());
                    comp.setSiteId(feeder.getId());
                    comp.setType(componentType);
                    data.addComponent(comp, true);
                    break;
                case 1:
                    comp = comps.get(0);
                    data.addComponent(comp, false);
                    break;
                default:
                    listener.reportMessage(String.format("Multiple matches for component %s of type %s for pole %s", compId, componentType, poleId));
                    return;
            }
            comp.setModel(StringUtil.nullableToString(featureProps.get(PROP_MODEL)));
            comp.setUtilityId(compId);
            removeKeys(featureProps, KEYS_TO_REMOVE);
            for (String key : featureProps.keySet()) {
                comp.getAttributes().put(key, StringUtil.nullableToString(featureProps.get(key)));
            }
        } catch (IOException ex) {
            listener.reportNonFatalException(String.format("Error parsing data for %s component of pole %s", componentType, poleId), ex);
        }
    }
}

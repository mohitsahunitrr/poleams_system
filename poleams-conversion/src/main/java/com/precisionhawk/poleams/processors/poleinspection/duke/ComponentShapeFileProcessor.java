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
    private final ComponentType componentType;
    private final boolean expectOneFeeder;

    public ComponentShapeFileProcessor(WSClientHelper svcs, ProcessListener listener, InspectionData data, File shapeFile, ComponentType componentType, boolean expectOneFeeder) {
        super(svcs, listener, data, shapeFile);
        this.componentType = componentType;
        this.expectOneFeeder = expectOneFeeder;
    }

    @Override
    protected void processFeature(Map<String, Object> featureProps) {
        String compId = longIntegerAsString(firstOf(featureProps, PROP_ID));
        String feederNumber = StringUtil.nullableToString(featureProps.get(PROP_NETWORK_ID));
        String poleId = longIntegerAsString(firstOf(featureProps, PROP_POLE_ID));
        String poleSerial = StringUtil.nullableToString(featureProps.get(PROP_POLE_NUM));
        
        if ((poleId == null || poleId.isEmpty()) && (poleSerial == null || poleSerial.isEmpty())) {
            listener.reportMessage(String.format("Pole ID missing for %s %s", componentType, compId));
            return;
        }
        
        try {
            Feeder feeder = data.getFeedersByFeederNum().get(feederNumber);
            if (feeder == null) {
                FeederSearchParams fparams = new FeederSearchParams();
                fparams.setFeederNumber(feederNumber);
                if (fparams.hasCriteria()) {
                    feeder = CollectionsUtilities.firstItemIn(svcs.feeders().search(svcs.token(), fparams));
                }
                if (feeder == null) {
                    if (expectOneFeeder && data.getFeedersByFeederNum().size() == 1) {
                        feeder = data.getCurrentFeeder();
                    } else {
                        listener.reportNonFatalError(String.format("Unable to locate feeder %s", feederNumber));
                        return;
                    }
                } else {
                    data.addFeeder(feeder, false);
                }
            }
            data.setCurrentFeeder(feeder);
            
            Pole pole = null;
            if (StringUtil.notNullNotEmpty(poleId)) {
                pole = data.getPolesMap().get(new SiteAssetKey(feeder.getId(), poleId));
            } else {
                pole = data.getPolesMap().get(new SiteAssetKey(feeder.getId(), poleSerial));
            }
            if (pole == null) {
                PoleSearchParams pparams = new PoleSearchParams();
                pparams.setSiteId(feeder.getId());
                if (poleId != null && !poleId.isEmpty()) {
                    pparams.setUtilityId(poleId);
                } else {
                    pparams.setSerialNumber(poleSerial);
                }
                pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
                if (pole == null) {
                    listener.reportNonFatalError(String.format("Unable to locate pole %s for feeder %s", StringUtil.notNullNotEmpty(poleId) ? poleId : poleSerial, feederNumber));
                    return;
                }
                data.addPole(pole, false);
            }
            
            // If compId = null, we trust that there is only one component of the given type for any given pole.
            ComponentSearchParams cparams = new ComponentSearchParams();
            cparams.setAssetId(pole.getId());
            cparams.setType(componentType);
            cparams.setUtilityId(compId);
            if (!cparams.hasCriteria()) {
                listener.reportNonFatalError(String.format("Unable to locate pole %s for feeder %s", poleId, feederNumber));
                return;
            }
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

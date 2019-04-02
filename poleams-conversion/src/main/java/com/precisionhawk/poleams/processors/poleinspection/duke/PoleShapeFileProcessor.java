package com.precisionhawk.poleams.processors.poleinspection.duke;

import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.processors.DataImportUtilities;
import com.precisionhawk.poleams.processors.InspectionData;
import com.precisionhawk.poleams.processors.ProcessListener;
import com.precisionhawk.poleams.processors.ShapeFileProcessor;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import org.papernapkin.liana.util.IntegerUtil;
import org.papernapkin.liana.util.StringUtil;

/**
 *
 * @author pchapman
 */
public class PoleShapeFileProcessor extends ShapeFileProcessor implements ShapeFileConstants {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String PROP_CLASS = "CLASS";
    private static final String PROP_HEIGHT = "HEIGHT";
    private static final String PROP_INSTALL_DATE = "DATE_INSTA";
    private static final Collection<String> KEYS_TO_REMOVE = new LinkedList();
    static {
        KEYS_TO_REMOVE.addAll(Arrays.asList(PROPS_TO_REMOVE));
        KEYS_TO_REMOVE.add(PROP_CLASS);
        KEYS_TO_REMOVE.add(PROP_HEIGHT);
    }
    
    public PoleShapeFileProcessor(WSClientHelper svcs, ProcessListener listener, InspectionData data, File shapeFile) {
        super(svcs, listener, data, shapeFile);
    }

    @Override
    protected void processFeature(Map<String, Object> featureProps) {
        String poleId = null;
        try {
            String poleClass = StringUtil.nullableToString(featureProps.get(PROP_CLASS));
            String height = StringUtil.nullableToString(featureProps.get(PROP_HEIGHT));
            poleId = StringUtil.nullableToString(firstOf(featureProps, PROP_POLE_ID));
            String poleName = StringUtil.nullableToString(featureProps.get(PROP_POLE_NUM));
            String feederNumber = StringUtil.nullableToString(featureProps.get(PROP_NETWORK_ID));
            String serialNumber = poleName;
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
            
            if (data.getCurrentWorkOrder() != null) {
                DataImportUtilities.ensureFeederInspection(svcs, data, listener, feeder, InspectionStatuses.SI_PENDING);
            } else {
                data.setCurrentFeederInspection(null);
            }
            
            PoleSearchParams pparams = new PoleSearchParams();
            pparams.setSiteId(feeder.getId());
            pparams.setUtilityId(poleId);
            Pole pole = CollectionsUtilities.firstItemIn(svcs.poles().search(svcs.token(), pparams));
            if (pole == null) {
                pole = new Pole();
                pole.setId(UUID.randomUUID().toString());
                pole.setName(poleName);
                pole.setSiteId(feeder.getId());
                pole.setUtilityId(poleId);
                data.addPole(pole, true);
            } else {
                data.addPole(pole, false);
            }
            pole.setDateOfInstall(localDate(featureProps, PROP_INSTALL_DATE));
            pole.setLength(IntegerUtil.parseIntegerSafe(height));
            pole.setLocation(super.location(featureProps));
            pole.setPoleClass(poleClass);
            pole.setSerialNumber(serialNumber);
            removeKeys(featureProps, KEYS_TO_REMOVE);
            for (String key : featureProps.keySet()) {
                pole.getAttributes().put(key, StringUtil.nullableToString(featureProps.get(key)));
            }
            
            if (data.getCurrentFeederInspection() != null) {
                PoleInspection insp = DataImportUtilities.ensurePoleInspection(svcs, listener, data, pole, null, InspectionStatuses.AI_PENDING);
            }
        } catch (IOException ex) {
            listener.reportNonFatalException(String.format("Error parsing data for pole %s", poleId), ex);
        }
    }
    
    private LocalDate localDate(Map<String, Object> featureProps, String key) {
        LocalDate d = null;
        String s = longIntegerAsString(featureProps.get(key));
        if (s == null) {
            return null;
        }
        if (s.length() > 6) {
            s = s.substring(0, 5);
        }
        return LocalDate.parse(s, DATE_FORMATTER);
    }
}

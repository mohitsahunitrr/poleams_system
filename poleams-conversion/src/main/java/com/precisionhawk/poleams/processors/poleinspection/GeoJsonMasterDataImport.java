package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.bean.GeoPoint;
import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import static org.codehaus.jackson.JsonToken.*;

/**
 *
 * @author pchapman
 */
public class GeoJsonMasterDataImport extends AbstractInspectionImport {
    
    private static final String FIELD_CLASS = "CLASS";
    private static final String FIELD_COORDS = "coordinates";
    private static final String FIELD_FEATURES = "features";
    private static final String FIELD_GEOMETRY = "geometry";
    private static final String FIELD_HEIGHT = "HEIGHT";
    private static final String FIELD_POLEID = "POLEID";
    private static final String FIELD_POLENUM = "POLE_NUMBE";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_LC_TYPE = "type";

    public boolean process(Environment env, ImportProcessListener listener, String feederId, String orderNum, File poleDataJson) {
        boolean success = true;
        
        JsonParser parser = null;
        try {
            InspectionData data = new InspectionData();
            
            // Feeder
            SubStationSearchParameters params = new SubStationSearchParameters();
            params.setFeederNumber(feederId);
            data.setSubStation(CollectionsUtilities.firstItemIn(env.obtainWebService(SubStationWebService.class).search(env.obtainAccessToken(), params)));
            if (data.getSubStation() == null) {
                listener.reportFatalError(String.format("Unable to locate feeder %s", feederId));
                return false;
            }
            data.getDomainObjectIsNew().put(data.getSubStation().getId(), false);
            
            JsonFactory jfactory = new JsonFactory();
            parser = jfactory.createJsonParser(poleDataJson);

            // Find the array of features
            success = advanceToField(parser, FIELD_FEATURES);
            // Find the beginning of the array
            success = success && advanceToArrayStart(parser);
            if (success) {
                // As long as we jave pole objects, continue
                while (success && advanceToObjectStartOrArrayEnd(parser) == JsonToken.START_OBJECT) {
                    success = processObjectData(env, listener, data, parser);
                }
                if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                    // Everything is as expected.  We do not need data from the remainder of the JSON document.
                    listener.reportMessage("The end of the pole data has been reached.");
                } else {
                    listener.reportMessage(String.format("Unexepected JSON token %s", parser.getCurrentToken()));
                    return false;
                }
            }
            
            listener.reportMessage("Saving data...");
            savePoleData(env, listener, data);
            saveAndUploadResources(env, listener, data);

            return success;
        } catch (IOException ex) {
            listener.reportFatalException(ex);
        } finally {
            IOUtils.closeQuietly(parser);
        }
        return true;
    }
    
    private boolean advanceToArrayStart(JsonParser parser) throws IOException {
        for (JsonToken token = parser.nextToken(); token != JsonToken.START_ARRAY && token != JsonToken.NOT_AVAILABLE && token != null; token = parser.nextToken()) {
            // Loop again
        }
        return parser.getCurrentToken() == JsonToken.START_ARRAY;
    }
    
    private boolean advanceToNextField(JsonParser parser, boolean stopAtEndOfObject) throws IOException {
        for (
                JsonToken token = parser.nextToken()
                ;
                token != JsonToken.FIELD_NAME
                && token != JsonToken.NOT_AVAILABLE
                && token != null && (!stopAtEndOfObject || token != JsonToken.END_OBJECT)
                ;
                token = parser.nextToken()
             )
        {
            // Loop again
        }
        return parser.getCurrentToken() == JsonToken.FIELD_NAME;
    }
    
    private boolean advanceToField(JsonParser parser, String fieldName) throws IOException {
        while (advanceToNextField(parser, false)) {
            if (fieldName.equals(parser.getCurrentName())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean advanceToObjectEnd(JsonParser parser) throws IOException {
        for (JsonToken token = parser.nextToken(); token != JsonToken.END_OBJECT && token != JsonToken.NOT_AVAILABLE && token != null; token = parser.nextToken()) {
            // Loop again
        }
        return parser.getCurrentToken() == JsonToken.END_OBJECT;
    }
    
    private boolean advanceToObjectStart(JsonParser parser) throws IOException {
        for (JsonToken token = parser.nextToken(); token != JsonToken.START_OBJECT && token != JsonToken.NOT_AVAILABLE && token != null; token = parser.nextToken()) {
            // Loop again
        }
        return parser.getCurrentToken() == JsonToken.START_OBJECT;
    }

    private JsonToken advanceToObjectStartOrArrayEnd(JsonParser parser) throws IOException {
        for (
                JsonToken token = parser.nextToken()
                ;
                token != JsonToken.START_OBJECT && token != JsonToken.END_ARRAY && token != JsonToken.NOT_AVAILABLE && token != null
                ;
                token = parser.nextToken()
            )
        {
            // Loop again
        }
        return parser.getCurrentToken();
    }

    private boolean processObjectData(Environment env, ProcessListener listener, InspectionData data, JsonParser parser) throws IOException {
        GeoPoint location = null;
        String poleId;
        Map<String, String> attributes = null;
        while (advanceToNextField(parser, true)) {
            String fieldName = parser.getCurrentName();
            switch (fieldName) {
                case FIELD_GEOMETRY:
                    location = parseLocation(listener, parser);
                    break;
                case FIELD_LC_TYPE:
                    // Skip
                    break;
                case FIELD_PROPERTIES:
                    attributes = parseProperties(listener, parser);
                    break;
            }
        }
        if (attributes == null) {
            listener.reportFatalError("Bad or missing attributes");
            return false;
        } else {
            poleId = attributes.remove(FIELD_POLEID);
        }
        if (poleId == null) {
            listener.reportFatalError("Bad or missing POLEID");
            return false;
        }
        if (location == null) {
            listener.reportFatalError(String.format("Bad or missing location for pole %s", poleId));
        }
        PoleSearchParameters params = new PoleSearchParameters();
        params.setSubStationId(data.getSubStation().getId());
        params.setFPLId(poleId);
        listener.reportMessage(String.format("Loaded pole with ID %s", poleId));
        Pole pole = CollectionsUtilities.firstItemIn(env.obtainWebService(PoleWebService.class).search(env.obtainAccessToken(), params));
        boolean isnew = pole == null;
        if (isnew) {
            pole = new Pole();
            pole.setId(UUID.randomUUID().toString());
            String s = attributes.remove(FIELD_HEIGHT);
            try {
                pole.setLength(s == null ? null : Integer.valueOf(s));
            } catch (NumberFormatException ex) {
                listener.reportNonFatalError(String.format("Invalid value for HEIGHT: %s", s));
            }
            pole.setLocation(location);
            pole.setOrganizationId(data.getSubStation().getOrganizationId());
            pole.setPoleClass(attributes.get(FIELD_CLASS));
            pole.setFPLId(poleId);
            pole.setType(attributes.remove("TYPE"));
            pole.setSubStationId(data.getSubStation().getId());
            data.addPole(pole, true);
        } else {
            pole.setOrganizationId(data.getSubStation().getOrganizationId());
            data.addPole(pole, false);
        }
        
        PoleInspection insp = null;
        if (!isnew) {
            // try to look up a pole inspection.
            PoleInspectionSearchParameters aiparams = new PoleInspectionSearchParameters();
            aiparams.setPoleId(pole.getId());
            insp = CollectionsUtilities.firstItemIn(env.obtainWebService(PoleInspectionWebService.class).search(env.obtainAccessToken(), aiparams));
        }
        if (insp == null) {
            insp = new PoleInspection();
            insp.setPoleId(pole.getId());
            insp.setOrganizationId(pole.getOrganizationId());
            insp.setId(UUID.randomUUID().toString());
            insp.setSubStationId(data.getSubStation().getId());
            data.addPoleInspection(pole, insp, true);
        } else {
            data.addPoleInspection(pole, insp, false);
        }
        
        return true;
    }

    private GeoPoint parseLocation(ProcessListener listener, JsonParser parser) throws IOException {
        if (advanceToField(parser, FIELD_COORDS)) {
            if (advanceToArrayStart(parser)) {
                Float lat = null;
                Float lon = null;
                JsonToken token = parser.nextToken();
                if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                    lon = parser.getFloatValue();
                }
                token = parser.nextValue();
                if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                    lat = parser.getFloatValue();
                }
                if (advanceToObjectEnd(parser)) {
                    if (lat != null && lon != null) {
                        GeoPoint point = new GeoPoint();
                        point.setLatitude(Double.valueOf(lat));
                        point.setLongitude(Double.valueOf(lon));
                        return point;
                    }
                }
                listener.reportFatalError("Invalid coordinates.");
            }
        }
        return null;
    }

    private Map<String, String> parseProperties(ProcessListener listener, JsonParser parser) throws IOException {
        if (advanceToObjectStart(parser)) {
            Map<String, String> data = new HashMap<>();
            JsonToken token;
            String fieldName;
            while (advanceToNextField(parser, true)) {
                fieldName = parser.getCurrentName();
                token = parser.nextToken();
                switch (token) {
                    case VALUE_FALSE:
                        data.put(fieldName, Boolean.FALSE.toString());
                        break;
                    case VALUE_TRUE:
                        data.put(fieldName, Boolean.TRUE.toString());
                        break;
                    case VALUE_NULL:
                        data.put(fieldName, null);
                        break;
                    case VALUE_NUMBER_FLOAT:
                        data.put(fieldName, Float.toString(parser.getFloatValue()));
                        break;
                    case VALUE_NUMBER_INT:
                        data.put(fieldName, Integer.toString(parser.getIntValue()));
                        break;
                    case VALUE_STRING:
                        data.put(fieldName, parser.getText());
                        break;
                    default:
                        listener.reportNonFatalError(String.format("Unknown value for field %s", fieldName));
                }
            }
            return data;
        }
        return null;
    }
}

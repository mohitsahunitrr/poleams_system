package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.ams.bean.GeoPoint;
import com.precisionhawk.ams.webservices.client.Environment;
import com.precisionhawk.poleams.webservices.client.WSClientHelper;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 *
 * @author pchapman
 */
public class GeoJsonMasterDataImport {
    
    private static final String FIELD_CLASS = "CLASS";
    private static final String FIELD_COORDS = "coordinates";
    private static final String FIELD_FEATURES = "features";
    private static final String FIELD_GEOMETRY = "geometry";
    private static final String FIELD_HEIGHT = "HEIGHT";
    private static final String FIELD_POLEID = "POLEID";
    private static final String FIELD_POLENUM = "POLE_NUMBE";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_LC_TYPE = "type";

    public boolean process(Environment env, ImportProcessListener listener, String feederId, File poleDataJson) {
        boolean success = true;
        
        JsonParser parser = null;
        try {
            listener.setStatus(ImportProcessStatus.Initializing);
            WSClientHelper svcs = new WSClientHelper(env);
            InspectionData data = new InspectionData();
            data.setFeeder(svcs.feeders().retrieve(svcs.token(), feederId));
            if (data.getFeeder() == null) {
                listener.reportFatalError(String.format("Unable to locate feeder %s", feederId));
                return false;
            }
            data.getDomainObjectIsNew().put(data.getFeeder().getId(), false);
            JsonFactory jfactory = new JsonFactory();
            listener.setStatus(ImportProcessStatus.ProcessingPoleData);
            parser = jfactory.createJsonParser(poleDataJson);

            // Find the array of features
            success = advanceToField(parser, FIELD_FEATURES);
            // Find the beginning of the array
            success = success && advanceToArrayStart(parser);
            if (success) {
                // As long as we jave pole objects, continue
                while (success && advanceToObjectStart(parser)) {
                    success = processObjectData(svcs, listener, data, parser);
                }
            }

            return success;
        } catch (IOException ex) {
            //TODO:
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
    
    private boolean advanceToObjectStart(JsonParser parser) throws IOException {
        for (JsonToken token = parser.nextToken(); token != JsonToken.START_OBJECT && token != JsonToken.NOT_AVAILABLE && token != null; token = parser.nextToken()) {
            // Loop again
        }
        return parser.getCurrentToken() == JsonToken.START_OBJECT;
    }

    private boolean processObjectData(WSClientHelper svcs, ImportProcessListener listener, InspectionData data, JsonParser parser) throws IOException {
        GeoPoint location = null;
        String poleId = null;
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
        return true;
    }

    private GeoPoint parseLocation(ImportProcessListener listener, JsonParser parser) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Map<String, String> parseProperties(ImportProcessListener listener, JsonParser parser) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

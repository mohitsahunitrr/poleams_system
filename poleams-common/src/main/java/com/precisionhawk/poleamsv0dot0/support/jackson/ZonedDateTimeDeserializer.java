package com.precisionhawk.poleamsv0dot0.support.jackson;

import com.precisionhawk.poleamsv0dot0.support.time.TimeFormattingConstants;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Philip A. Chapman
 */
public class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> implements TimeFormattingConstants {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public ZonedDateTime deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        String s = mapper.readValue(dc.getParser(), String.class);
        if (s == null || s.length() == 0) {
            return null;
        } else {
            return ZonedDateTime.parse(s, DATE_TIME_FORMATTER);
        }
    }
    
}

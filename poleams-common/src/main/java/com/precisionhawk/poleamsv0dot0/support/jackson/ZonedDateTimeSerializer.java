package com.precisionhawk.poleamsv0dot0.support.jackson;

import com.precisionhawk.poleamsv0dot0.support.time.TimeFormattingConstants;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 *
 * @author Philip A. Chapman
 */
public class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> implements TimeFormattingConstants {

    @Override
    public void serialize(ZonedDateTime t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
        String s = t.format(DATE_TIME_FORMATTER);
        jg.writeString(s);
    }
    
}

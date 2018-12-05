package com.precisionhawk.poleams.support.jackson;

import com.precisionhawk.poleams.support.time.TimeFormattingConstants;
import java.io.IOException;
import java.time.LocalDate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 *
 * @author Philip A. Chapman
 */
public class LocalDateSerializer extends JsonSerializer<LocalDate> implements TimeFormattingConstants {

    @Override
    public void serialize(LocalDate t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
        String s = t.format(DATE_FORMATTER);
        jg.writeString(s);
    }
    
}

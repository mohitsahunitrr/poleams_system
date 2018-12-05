package com.precisionhawk.poleams.support.jackson;

import java.io.IOException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.papernapkin.liana.util.BASE64Decoder;

/**
 *
 * @author Philip A. Chapman
 */
public class ByteArrayDeserializer extends JsonDeserializer<byte[]> {
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final BASE64Decoder decoder = new BASE64Decoder();
    
    @Override
    public byte[] deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
        String s = mapper.readValue(dc.getParser(), String.class);
        if (s == null || s.length() == 0) {
            return null;
        } else {
            return decoder.decodeBuffer(s);
        }
    }
}

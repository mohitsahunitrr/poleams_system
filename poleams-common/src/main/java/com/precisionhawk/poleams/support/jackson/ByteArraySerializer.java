package com.precisionhawk.poleams.support.jackson;

import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.papernapkin.liana.util.BASE64Encoder;

/**
 *
 * @author Philip A. Chapman
 */
public class ByteArraySerializer extends JsonSerializer<byte[]> {
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final BASE64Encoder encoder = new BASE64Encoder();

    @Override
    public void serialize(byte[] data, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException
    {
        String s = encoder.encode(data);
        jg.writeString(s);
    }
}

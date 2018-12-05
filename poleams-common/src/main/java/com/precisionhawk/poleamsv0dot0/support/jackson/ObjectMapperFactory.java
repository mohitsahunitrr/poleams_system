/*
 * All rights reserved.
 */

package com.precisionhawk.poleamsv0dot0.support.jackson;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import javax.inject.Provider;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class ObjectMapperFactory implements Provider<ObjectMapper> {

    private final ObjectMapper objectMapper;
    
    public ObjectMapperFactory() {
        objectMapper = new ObjectMapper();
        SimpleModule module;
        
        // Date
        module = new SimpleModule("dateModule", new Version(1,0,0,null));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        objectMapper.registerModule(module);
        
        // Datetime
        module = new SimpleModule("dateTimeModule", new Version(1,0,0,null));
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        objectMapper.registerModule(module);
        
        // Time
        module = new SimpleModule("timeModule", new Version(1,0,0,null));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        module.addSerializer(LocalTime.class, new LocalTimeSerializer());
        objectMapper.registerModule(module);
    }
    
    @Override
    public ObjectMapper get() {
        return objectMapper;
    }
}

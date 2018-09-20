/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.support.jackson;

import javax.inject.Provider;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class ObjectMapperFactory implements Provider<ObjectMapper> {

    private final ObjectMapper objectMapper;
    
    public ObjectMapperFactory() {
        objectMapper = new ObjectMapper();
    }
    
    @Override
    public ObjectMapper get() {
        return objectMapper;
    }
}

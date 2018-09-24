package com.precisionhawk.poleams.webservices.impl;

import javax.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philip A. Chapman
 */
public abstract class AbstractWebService {
    
    protected Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    protected static void ensureExists(Object obj, String errMsg) {
        if (
                obj == null
                ||
                (obj instanceof String && ((String)obj).isEmpty())
            )
        {
            throw new BadRequestException(errMsg);
        }
    }
}

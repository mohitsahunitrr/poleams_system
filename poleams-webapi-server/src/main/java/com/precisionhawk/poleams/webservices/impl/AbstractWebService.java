package com.precisionhawk.poleams.webservices.impl;

import java.util.List;
import javax.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pchapman
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
    
    protected static <T> T firstItemIn(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
}

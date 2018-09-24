package com.precisionhawk.poleams.support.resteasy;

import com.precisionhawk.poleams.support.jackson.ObjectMapperFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;

/**
 *
 * @author Philip A. Chapman
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JacksonProvider extends ResteasyJacksonProvider {
    
    public JacksonProvider() {
        setMapper(new ObjectMapperFactory().get());
    }
}

package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.ams.domain.ComponentInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.poleams.dao.ComponentInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.webservices.ComponentInspectionWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

/**
 *
 * @author pchapman
 */
@Named
public class ComponentInspectionWebServiceImpl extends AbstractWebService implements ComponentInspectionWebService {
    
    @Inject private ComponentInspectionDao ciDao;
    @Inject private ResourceWebService resourceService;

    @Override
    public ComponentInspection create(String authToken, ComponentInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The component inspection is required.");
        authorize(sess, inspection);
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            if (ciDao.insert(inspection)) {
                return inspection;
            } else {
                throw new BadRequestException(String.format("The component inspection %s already exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting component inspection.", ex);
        }
    }

    @Override
    public ComponentInspection retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "The component inspection ID is required.");
        try {
            return authorize(sess, validateFound(ciDao.retrieve(id)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving component inspection.", ex);
        }
    }

    @Override
    public List<ComponentInspection> search(String authToken, ComponentInspectionSearchParams params) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(params, "Search parameters are required.");
        authorize(sess, params);
        try {
            return authorize(sess, validateFound(ciDao.search(params)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving component inspections based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, ComponentInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The component inspection is required.");
        ensureExists(inspection.getId(), "The component inspection ID is required.");
        try {
            boolean updated = false;
            ComponentInspection p = ciDao.retrieve(inspection.getId());
            if (p != null) {
                authorize(sess, p);
                updated = ciDao.update(inspection);
            }
            if (!updated) {
                throw new NotFoundException(String.format("No component inspection with ID %s exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting component inspection.", ex);
        }
    }

    @Override
    public void delete(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        // See if it even exists
        ComponentInspection insp = retrieve(authToken, id);
        if (insp != null) {
            authorize(sess, insp);
            {
                // TODO: InspectionEventResources
                
                // TODO: Inspection Events
                
                // Delete any related resources
                ResourceSearchParams params = new ResourceSearchParams();
                params.setComponentInspectionId(insp.getId());
                for (ResourceMetadata rmeta : resourceService.search(authToken, params)) {
                    resourceService.delete(authToken, rmeta.getResourceId());
                }
            }
            try {
                // Delete the inspection itself.
                ciDao.delete(id);
            } catch (DaoException ex) {
                throw new InternalServerErrorException(String.format("Unable to delete component inspection %s", id), ex);
            }
        }
    }
    
}

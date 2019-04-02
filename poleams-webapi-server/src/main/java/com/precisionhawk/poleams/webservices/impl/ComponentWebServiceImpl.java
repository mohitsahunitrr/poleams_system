package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.ComponentSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.Component;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.dao.ComponentDao;
import com.precisionhawk.poleams.webservices.ComponentWebService;
import com.precisionhawk.poleams.webservices.ResourceWebService;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

/**
 *
 * @author pchapman
 */
@Named
public class ComponentWebServiceImpl extends AbstractWebService implements ComponentWebService {
    
    @Inject
    private ComponentDao componentDao;
    @Inject
    private ResourceWebService resourceSvc;

    @Override
    public Component retrieve(String authToken, String id) {
        ensureExists(id, "The component ID is required.");
        try {
            return componentDao.retrieve(id);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving component.", ex);
        }
    }

    @Override
    public List<Component> query(String authToken, ComponentSearchParams params) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Component create(String authToken, Component comp) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(comp, "The component is required.");
        authorize(sess, comp);
        if (comp.getId() == null) {
            comp.setId(UUID.randomUUID().toString());
        }
        try {
            if (componentDao.insert(comp)) {
                return comp;
            } else {
                throw new BadRequestException(String.format("The component %s already exists.", comp.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting component.", ex);
        }
    }

    @Override
    public void delete(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Component ID is required");
        // First, see if we even have such a component
        Component c = retrieve(authToken, id);
        authorize(sess, c);
        if (c != null) {
//            {
//                // Delete Inspections, which will delete any shared resources.
//                ComponentInspectionSearchParams params = new ComponentInspectionSearchParams();
//                params.setAssetId(id);
//                for (ComponentInspection insp : componentInspectionSvc.search(authToken, params)) {
//                    componentInspectionSvc.delete(authToken, insp.getId());
//                }
//            }
            {
                // Delete any resources left that are related to the pole.
                ResourceSearchParams params = new ResourceSearchParams();
                params.setComponentId(id);
                for (ResourceMetadata rmeta : resourceSvc.search(authToken, params)) {
                    resourceSvc.delete(authToken, rmeta.getResourceId());
                }
            }
            try {
                // Finally, we can delete the component.
                componentDao.delete(id);
            } catch (DaoException ex) {
                throw new InternalServerErrorException(String.format("Error deleting the component %s.\n", id), ex);
            }
        }
    }

    @Override
    public void update(String authToken, Component component) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

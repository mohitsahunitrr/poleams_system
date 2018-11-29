package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.bean.TransmissionStructureSummary;
import com.precisionhawk.poleams.dao.TransmissionStructureDao;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
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
public class TransmissionStructureWebServiceImpl extends AbstractWebService implements TransmissionStructureWebService {

    @Inject
    private TransmissionStructureDao dao;
    
    @Override
    public TransmissionStructure create(String authToken, TransmissionStructure structure) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(structure, "Transmission structure is required.");
        authorize(sess, structure);
        if (structure.getId() == null) {
            structure.setId(UUID.randomUUID().toString());
        }
        try {
            if (!dao.insert(structure)) {
                throw new BadRequestException(String.format("A transmission structure with the ID %s already exists.", structure.getId()));
            }
            return structure;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission structure data.", ex);
        }
    }

    @Override
    public TransmissionStructure retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Transmission structure ID is required.");
        try {
            TransmissionStructure s = dao.retrieve(id);
            if (s == null) {
                throw new NotFoundException(String.format("No transmission structure with ID %s found.", id));
            }
            return authorize(sess, s);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error loading the transmission structure data.", ex);
        }
    }

    @Override
    public TransmissionStructureSummary retrieveSummary(String authToken, String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<TransmissionStructure> search(String authToken, TransmissionStructureSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        if (searchParams == null || (!searchParams.hasCriteria())) {
            throw new BadRequestException("Transmission structure ID is required.");
        }
        authorize(sess, searchParams);
        try {
            return authorize(sess, dao.search(searchParams));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error loading the transmission structure data.", ex);
        }
    }

    @Override
    public void update(String authToken, TransmissionStructure structure) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(structure, "Transmission structure is required.");
        ensureExists(structure.getId(), "Transmission structure ID is required.");
        authorize(sess, structure);
        try {
            if (!dao.update(structure)) {
                throw new BadRequestException(String.format("A transmission structure with the ID %s does not exist.", structure.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission structure data.", ex);
        }
    }

    @Override
    public void delete(String authToken, String id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

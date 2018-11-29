package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.bean.TransmissionStructureInspectionSummary;
import com.precisionhawk.poleams.dao.TransmissionStructureInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
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
public class TransmissionStructureInspectionWebServiceImpl extends AbstractWebService implements TransmissionStructureInspectionWebService {

    @Inject
    private TransmissionStructureInspectionDao dao;
    
    @Override
    public TransmissionStructureInspection create(String authToken, TransmissionStructureInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission structure inspection is required.");
        authorize(sess, inspection);
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            if (!dao.insert(inspection)) {
                throw new BadRequestException(String.format("A transmission structure inspection with the ID %s already exists.", inspection.getId()));
            }
            return inspection;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission structure inspection data.", ex);
        }
    }

    @Override
    public TransmissionStructureInspection retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Transmission structure inspection ID is required.");
        try {
            TransmissionStructureInspection i = dao.retrieve(id);
            if (i == null) {
                throw new NotFoundException(String.format("No transmission structure inspection with ID %s found.", id));
            }
            return authorize(sess, i);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error loading the transmission structure inspection data.", ex);
        }
    }

    @Override
    public TransmissionStructureInspectionSummary retrieveSummary(String authToken, String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<TransmissionStructureInspection> search(String authToken, AssetInspectionSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        if (searchParams == null || (!searchParams.hasCriteria())) {
            throw new BadRequestException("Transmission structure ID is required.");
        }
        authorize(sess, searchParams);
        try {
            return authorize(sess, dao.search(searchParams));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error loading the transmission structure inspection data.", ex);
        }
    }

    @Override
    public void update(String authToken, TransmissionStructureInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission structure inspection is required.");
        ensureExists(inspection.getId(), "Transmission structure inspection ID is required.");
        authorize(sess, inspection);
        try {
            if (!dao.insert(inspection)) {
                throw new BadRequestException(String.format("A transmission structure inspection with the ID %s does not exist.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission structure inspection data.", ex);
        }
    }

    @Override
    public void delete(String authToken, String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

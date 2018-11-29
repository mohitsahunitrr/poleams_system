package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.bean.TransmissionLineInspectionSummary;
import com.precisionhawk.poleams.dao.TransmissionLineInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
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
public class TransmissionLineInspectionWebServiceImpl extends AbstractWebService implements TransmissionLineInspectionWebService {
    
    @Inject
    private TransmissionLineInspectionDao dao;

    @Override
    public TransmissionLineInspection create(String authToken, TransmissionLineInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission line inspection is required.");
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            if (!dao.insert(inspection)) {
                throw new BadRequestException(String.format("An transmission line inspection with the ID %s already exists.", inspection.getId()));
            }
            return inspection;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to persist the transmission line inspection data.", ex);
        }
    }

    @Override
    public TransmissionLineInspection retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Transmission line inspection ID is required.");
        try {
            TransmissionLineInspection i = dao.retrieve(id);
            if (i == null) {
                throw new NotFoundException(String.format("No transmission line inspection with ID %s found.", id));
            }
            return authorize(sess, dao.retrieve(id));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to persist the transmission line inspection data.", ex);
        }
    }

    @Override
    public List<TransmissionLineInspection> search(String authToken, SiteInspectionSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        if (searchParams == null || (!searchParams.hasCriteria())) {
            throw new BadRequestException("Search parameters are required.");
        }
        try {
            List<TransmissionLineInspection> inspections = dao.search(searchParams);
            return authorize(sess, inspections);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to load the transmission line inspection data.", ex);
        }
    }

    @Override
    public TransmissionLineInspectionSummary retrieveSummary(String authToken, String id) {
        //TODO:
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(String authToken, TransmissionLineInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission line inspection is required.");
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            if (!dao.update(inspection)) {
                throw new NotFoundException(String.format("No transmission line inspection with the ID %s found.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to persist the transmission line inspection data.", ex);
        }
    }
    
}

package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.ams.webservices.impl.SiteWebServiceUtilities;
import com.precisionhawk.poleams.bean.TransmissionLineSearchParams;
import com.precisionhawk.poleams.dao.TransmissionLineDao;
import com.precisionhawk.poleams.domain.TransmissionLine;
import com.precisionhawk.poleams.webservices.TransmissionLineWebService;
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
public class TransmissionLineWebServiceImpl extends AbstractWebService implements TransmissionLineWebService {

    @Inject
    private TransmissionLineDao dao;
    
    @Override
    public TransmissionLine create(String authToken, TransmissionLine line) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(line, "Transmission line is required.");
        SiteWebServiceUtilities.authorizeSite(sess, line);
        if (line.getId() == null) {
            line.setId(UUID.randomUUID().toString());
        }
        try {
            if (!dao.insert(line)) {
                throw new BadRequestException(String.format("A transmission line with the ID %s already exists.", line.getId()));
            }
            return line;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission line data.", ex);
        }
    }

    @Override
    public TransmissionLine retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Transmission line ID is required.");
        try {
            TransmissionLine l = dao.retrieve(id);
            if (l == null) {
                throw new BadRequestException(String.format("A transmission line with the ID %s not found.", id));
            }
            return SiteWebServiceUtilities.authorizeSite(sess, l);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving the transmission line data.", ex);
        }
    }

    @Override
    public List<TransmissionLine> retrieveAll(String authToken) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        try {
            return SiteWebServiceUtilities.cleanseUnAuthorizedSites(sess, dao.retrieveAll());
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving the transmission line data.", ex);
        }
    }

    @Override
    public List<TransmissionLine> search(String authToken, TransmissionLineSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        if (searchParams == null || (!searchParams.hasCriteria())) {
            throw new BadRequestException("Search parameters are required.");
        }
        try {
            return SiteWebServiceUtilities.cleanseUnAuthorizedSites(sess, dao.search(searchParams));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving the transmission line data.", ex);
        }
    }

    @Override
    public void update(String authToken, TransmissionLine line) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(line, "Transmission line is required.");
        SiteWebServiceUtilities.authorizeSite(sess, line);
        try {
            if (!dao.update(line)) {
                throw new NotFoundException(String.format("A transmission line with the ID %s was not found.", line.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error storing the transmission line data.", ex);
        }
    }
    
}

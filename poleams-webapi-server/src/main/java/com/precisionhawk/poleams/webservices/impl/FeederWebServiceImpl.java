package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.webservices.AbstractWebService;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.domain.Feeder;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.dao.FeederDao;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class FeederWebServiceImpl extends AbstractWebService implements FeederWebService {

    @Inject private PoleInspectionWebServiceImpl poleInspectionService;
    @Inject private PoleWebServiceImpl poleService;
    @Inject private ResourceWebServiceImpl resourceService;
    @Inject private FeederDao substationDao;
    
    @Override
    public Feeder create(String authToken, Feeder substation) {
        ensureExists(substation, "The feeder is required.");
        if (substation.getId() == null) {
            substation.setId(UUID.randomUUID().toString());
        }
        try {
            if (substationDao.insert(substation)) {
                return substation;
            } else {
                throw new BadRequestException(String.format("The feeder %s already exists.", substation.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting feeder.", ex);
        }
    }

    @Override
    public Feeder retrieve(String authToken, String substationId) {
        ensureExists(substationId, "Feeder ID is required.");
        try {
            return substationDao.retrieve(substationId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving feeder.", ex);
        }
    }

    @Override
    public List<Feeder> retrieveAll(String authToken) {
        try {
            return substationDao.retrieveAll();
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving feeders.", ex);
        }
    }

    @Override
    public void update(String authToken, Feeder substation) {
        ensureExists(substation, "The Feeder is required.");
        ensureExists(substation.getId(), "Feeder ID is required.");
        try {
            if (!substationDao.update(substation)) {
                throw new BadRequestException(String.format("The feeder %s already exists.", substation.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting feeder.", ex);
        }
    }

    @Override
    public List<Feeder> search(String authToken, FeederSearchParams searchParams) {
        ensureExists(searchParams, "Search parameters are required.");
        try {
            return substationDao.search(searchParams);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to search for feeders.", ex);
        }
    }
    
}

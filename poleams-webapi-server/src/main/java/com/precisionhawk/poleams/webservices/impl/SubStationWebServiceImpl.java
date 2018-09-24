package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSearchParameters;
import com.precisionhawk.poleams.bean.SubStationSummary;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.SubStationDao;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.domain.SubStation;
import com.precisionhawk.poleams.webservices.SubStationWebService;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class SubStationWebServiceImpl extends AbstractWebService implements SubStationWebService {

    @Inject private PoleInspectionWebServiceImpl poleInspectionService;
    @Inject private PoleWebServiceImpl poleService;
    @Inject private ResourceWebServiceImpl resourceService;
    @Inject private SubStationDao substationDao;
    
    @Override
    public SubStation create(String authToken, SubStation substation) {
        ensureExists(substation, "The SubStation is required.");
        if (substation.getId() == null) {
            substation.setId(UUID.randomUUID().toString());
        }
        try {
            if (substationDao.insert(substation)) {
                return substation;
            } else {
                throw new BadRequestException(String.format("The substation %s already exists.", substation.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting substation.", ex);
        }
    }

    @Override
    public SubStation retrieve(String authToken, String substationId) {
        ensureExists(substationId, "Substation ID is required.");
        try {
            return substationDao.retrieve(substationId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substation.", ex);
        }
    }

    @Override
    public List<SubStation> retrieveAll(String authToken) {
        try {
            return substationDao.retrieveAll();
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substations.", ex);
        }
    }

    @Override
    public SubStationSummary retrieveSummary(String authToken, String substationId) {
        SubStation ss = retrieve(authToken, substationId);

        ResourceSearchParameters rparams = new ResourceSearchParameters();
        rparams.setSubStationId(substationId);
        List<ResourceMetadata> resources;
        
        // Find the Feeder Map, if any.
        String feederMapDownloadURL = null;
        rparams.setType(ResourceType.FeederMap);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            feederMapDownloadURL = resourceService.getResourceDownloadURL(resources.get(0).getResourceId());
        }
        
        // Find Vegitation Encroachment Report, if any.
        String vegitationEncroachmentReportDownloadURL = null;
        rparams.setType(ResourceType.FeederMap);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            vegitationEncroachmentReportDownloadURL = resourceService.getResourceDownloadURL(resources.get(0).getResourceId());
        }
        
        // Load pole Summaries
        PoleSearchParameters pparams = new PoleSearchParameters();
        pparams.setSubStationId(substationId);
        List<PoleSummary> poleSummaries = poleService.retrieveSummaries(authToken, pparams);
        
        // Load pole inspection summaries
        PoleInspectionSearchParameters pisparams = new PoleInspectionSearchParameters();
        pisparams.setSubStationId(substationId);
        List<PoleInspectionSummary> poleInspectionSummaries = poleInspectionService.retrieveSummary(pisparams);
        
        return new SubStationSummary(ss, feederMapDownloadURL, vegitationEncroachmentReportDownloadURL, poleSummaries, poleInspectionSummaries);
    }

    @Override
    public List<SubStation> search(String authToken, SubStationSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            return substationDao.search(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substations by search parameters.", ex);
        }
    }

    @Override
    public void update(String authToken, SubStation substation) {
        ensureExists(substation, "The SubStation is required.");
        ensureExists(substation.getId(), "Substation ID is required.");
        try {
            if (!substationDao.update(substation)) {
                throw new BadRequestException(String.format("The substation %s already exists.", substation.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting substation.", ex);
        }
    }
    
}

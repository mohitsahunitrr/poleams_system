package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.webservices.AbstractWebService;
import com.precisionhawk.poleams.bean.PoleInspectionSearchParams;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.poleams.bean.FeederSearchParams;
import com.precisionhawk.poleams.bean.FeederSummary;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceTypes;
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
    public Feeder retrieve(String authToken, String substationId) {
        ensureExists(substationId, "Substation ID is required.");
        try {
            return substationDao.retrieve(substationId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substation.", ex);
        }
    }

    @Override
    public List<Feeder> retrieveAll(String authToken) {
        try {
            return substationDao.retrieveAll();
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substations.", ex);
        }
    }

    @Override
    public FeederSummary retrieveSummary(String authToken, String substationId) {
        Feeder ss = retrieve(authToken, substationId);

        // Load pole Summaries
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSubStationId(substationId);
        List<PoleSummary> poleSummaries = poleService.retrieveSummaries(authToken, pparams);
        
        // Load pole inspection summaries
        PoleInspectionSearchParams pisparams = new PoleInspectionSearchParams();
        pisparams.setSubStationId(substationId);
        List<PoleInspectionSummary> poleInspectionSummaries = poleInspectionService.retrieveSummary(authToken, pisparams);
        
        FeederSummary sss = new FeederSummary(ss, poleSummaries, poleInspectionSummaries);

        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setSiteId(substationId);
        List<ResourceMetadata> resources;
        
        // find the anomaly report, if any.
        rparams.setType(ResourceTypes.FeederAnomalyMap);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setAnomalyMapDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        // find the anomaly report, if any.
        rparams.setType(ResourceTypes.FeederAnomalyReport);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setAnomalyReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        // Find the Feeder Map, if any.
        rparams.setType(ResourceTypes.FeederMap);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setFeederMapDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        // find the Summary report, if any.
        rparams.setType(ResourceTypes.FeederSummaryReport);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setSummaryReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        // find the Survey report, if any.
        rparams.setType(ResourceTypes.SurveyReport);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setSurveyReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        //TODO: We should probably remove this
        // Find Vegitation Encroachment Report, if any.
        rparams.setType(ResourceTypes.EncroachmentReport);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setVegitationEncroachmentReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        // Find Vegitation Encroachment Report, if any.
        rparams.setType(ResourceTypes.EncroachmentShape);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setVegitationEncroachmentShapeDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId(), false));
        }
        
        return sss;
    }

    @Override
    public List<Feeder> search(String authToken, FeederSearchParams params) {
        ensureExists(params, "Search parameters are required.");
        try {
            return substationDao.search(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving substations by search parameters.", ex);
        }
    }

    @Override
    public void update(String authToken, Feeder substation) {
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

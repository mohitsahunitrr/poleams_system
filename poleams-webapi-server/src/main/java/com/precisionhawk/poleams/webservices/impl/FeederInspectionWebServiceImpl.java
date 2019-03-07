package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.util.Comparators;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.bean.FeederInspectionSummary;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.dao.FeederInspectionDao;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.webservices.FeederInspectionWebService;
import java.util.List;
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
public class FeederInspectionWebServiceImpl extends AbstractWebService implements FeederInspectionWebService {

    @Inject private PoleInspectionWebServiceImpl poleInspectionService;
    @Inject private PoleWebServiceImpl poleService;
    @Inject private ResourceWebServiceImpl resourceService;
    @Inject private FeederWebServiceImpl feederService;
    @Inject private FeederInspectionDao dao;

    @Override
    public FeederInspection create(String authToken, FeederInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The feeder inspection is required.");
        authorize(sess, inspection);
        try {
            if (dao.insert(inspection)) {
                return inspection;
            } else {
                throw new BadRequestException(String.format("The feeder inspection %s already exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting feeder.", ex);
        }
    }

    @Override
    public FeederInspection retrieve(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Feeder inspection ID is required.");
        try {
            return authorize(sess, validateFound(dao.retrieve(id)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException(String.format("Error retrieving feeder inspection %s.", id), ex);
        }
    }

    @Override
    public List<FeederInspection> search(String authToken, SiteInspectionSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(searchParams, "Search parameters are required.");
        authorize(sess, searchParams);
        try {
            return authorize(sess, dao.search(searchParams));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving feeder inspections.", ex);
        }
    }

    @Override
    public FeederInspectionSummary retrieveSummary(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        FeederInspection finsp = retrieve(authToken, id);
        authorize(sess, finsp);
        Feeder feeder = feederService.retrieve(authToken, finsp.getSiteId());

        // Load pole Summaries
        PoleSearchParams pparams = new PoleSearchParams();
        pparams.setSiteId(feeder.getId());
        List<PoleSummary> poleSummaries = poleService.retrieveSummaries(authToken, pparams);
        
        // Load pole inspection summaries
        AssetInspectionSearchParams pisparams = new AssetInspectionSearchParams();
        pisparams.setSiteInspectionId(id);
        List<PoleInspectionSummary> poleInspectionSummaries = poleInspectionService.retrieveSummary(sess, pisparams);

        FeederInspectionSummary sss = new FeederInspectionSummary(feeder, finsp, poleSummaries, poleInspectionSummaries);

        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setSiteInspectionId(id);
        List<ResourceMetadata> resources;
        
        // find flight videos, if any
        rparams.setType(ResourceTypes.FlightVideo);
        sss.setInspectionFlightVideos(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));
        
        // find the anomaly report, if any.        
        rparams.setType(ResourceTypes.FeederAnomalyMap);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setAnomalyMapDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // find the anomaly report, if any.
        rparams.setType(ResourceTypes.FeederAnomalyReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setAnomalyReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // find the Summary report, if any.
        rparams.setType(ResourceTypes.FeederSummaryReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setSummaryReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // find the Survey report, if any.
        rparams.setType(ResourceTypes.SurveyReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setSurveyReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        //TODO: We should probably remove this
        // Find Vegitation Encroachment Report, if any.
        rparams.setType(ResourceTypes.EncroachmentReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setVegitationEncroachmentReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // Find Vegitation Encroachment Report, if any.
        rparams.setType(ResourceTypes.EncroachmentShape);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setVegitationEncroachmentShapeDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // Find the Feeder Map, if any.
        rparams.setType(ResourceTypes.FeederMap);
        rparams.setSiteId(finsp.getSiteId());
        rparams.setSiteInspectionId(null);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            sss.setFeederMapDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        return sss;
    }

    @Override
    public void update(String authToken, FeederInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The feeder inspection is required.");
        ensureExists(inspection.getId(), "Feeder inspection ID is required.");
        authorize(sess, inspection);
        try {
            boolean updated = false;
            FeederInspection i = dao.retrieve(inspection.getId());
            if (i != null) {
                authorize(sess, i);
                updated = dao.update(inspection);
            }
            if (!updated) {
                throw new BadRequestException(String.format("The feeder inspection %s does not exist.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting feeder inspection.", ex);
        }
    }

    @Override
    public void delete(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Feeder inspection ID is required.");
        try {
            if (!dao.delete(id)) {
                throw new NotFoundException(String.format("No feeder inspection %s exists.", id));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving the feeder inspection data.", ex);
        }
    }
    
}

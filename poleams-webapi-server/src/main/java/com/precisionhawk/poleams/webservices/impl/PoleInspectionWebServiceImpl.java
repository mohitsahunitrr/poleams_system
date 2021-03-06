package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.ComponentInspectionSearchParams;
import com.precisionhawk.ams.bean.InspectionEventResourceSearchParams;
import com.precisionhawk.ams.bean.InspectionEventSearchParams;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.ComponentInspection;
import com.precisionhawk.ams.domain.InspectionEvent;
import com.precisionhawk.poleams.dao.PoleInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.domain.ResourceStatus;
import com.precisionhawk.ams.util.Comparators;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.dao.FeederDao;
import com.precisionhawk.poleams.dao.InspectionEventDao;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.ResourceTypes;
import com.precisionhawk.poleams.webservices.ComponentInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

/**
 *
 * @author Philip A. Chapman
 */
@Named
public class PoleInspectionWebServiceImpl extends AbstractWebService implements PoleInspectionWebService {
    
    //TODO: Remove boilderplate DaoException handling?
    
    @Inject private ComponentInspectionWebService componentInspectionService;
    @Inject private InspectionEventDao ieDao;
    @Inject private PoleInspectionDao piDao;
    @Inject private ResourceWebServiceImpl resourceService;
    @Inject private FeederDao feederDao;

    @Override
    public PoleInspection create(String authToken, PoleInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The pole inspection is required.");
        authorize(sess, inspection);
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            if (piDao.insert(inspection)) {
                return inspection;
            } else {
                throw new BadRequestException(String.format("The pole inspection %s already exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole inspection.", ex);
        }
    }

    @Override
    public PoleInspection retrieve(String authToken, String inspectionId) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspectionId, "The pole inspection ID is required.");
        try {
            return authorize(sess, validateFound(piDao.retrieve(inspectionId)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspection.", ex);
        }
    }

    //TODO: Remove me once we have business rules for criticality.
    private final Random random = new Random();
    
    @Override
    public PoleInspectionSummary retrieveSummary(String authToken, String inspectionId) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspectionId, "The pole inspection ID is required.");
        try {
            PoleInspection inspection = piDao.retrieve(inspectionId);
            authorize(sess, inspection);
            if (inspection == null) {
                return null;
            }
            
            return populateSummary(authToken, new PoleInspectionSummary(inspection));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspection data.", ex);
        }
    }

    @Override
    public List<PoleInspection> search(String authToken, AssetInspectionSearchParams params) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(params, "Search parameters are required.");
        authorize(sess, params);
        try {
            return authorize(sess, validateFound(piDao.search(params)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspections based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, PoleInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "The pole inspection is required.");
        ensureExists(inspection.getId(), "The pole inspection ID is required.");
        try {
            boolean updated = false;
            PoleInspection p = piDao.retrieve(inspection.getId());
            if (p != null) {
                authorize(sess, p);
                updated = piDao.update(inspection);
            }
            if (!updated) {
                throw new NotFoundException(String.format("No pole inspection with ID %s exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole inspection.", ex);
        }
    }
    
    
    List<PoleInspectionSummary> retrieveSummary(String authToken, AssetInspectionSearchParams params) {
        try {
            List<PoleInspection> inspections = piDao.search(params);
            List<PoleInspectionSummary> results = new ArrayList<>(inspections.size());
            for (PoleInspection inspection : inspections) {
                results.add(populateSummary(authToken, new PoleInspectionSummary(inspection)));
            }
            return results;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspection data.", ex);
        }
    }
    
    private Integer calculateCriticality(PoleInspectionSummary summary) {
        boolean isFPL = false;
        try {
            Feeder f = feederDao.retrieve(summary.getSiteId());
            //TODO: Put this in a constant somewhere
            isFPL = "9d718b1e-ca84-4e78-a1cb-1393ceecc927".equals(f.getOrganizationId());
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error looking up feeder.", ex);
        }
        if (isFPL) {
            // Based on horizontal loading %
            Integer i = summary.getHorizontalLoadingPercent();
            if (i == null) {
                return null;
            } else if (i < 90) {
                return 1;
            } else if (i < 100) {
                return 2;
            } else if (i < 120) {
                return 3;
            } else if (i < 200) {
                return 4;
            } else {
                return 5;
            }
        } else {
            try {
                int i = 0;
                InspectionEventSearchParams params = new InspectionEventResourceSearchParams();
                params.setAssetId(summary.getAssetId());
                params.setOrderNumber(summary.getOrderNumber());
                for (InspectionEvent evt : ieDao.search(params)) {
                    if (evt.getSeverity() != null && evt.getSeverity() > i) {
                        i = evt.getSeverity();
                    }
                }
                return i;
            } catch (DaoException ex) {
                throw new InternalServerErrorException("Error looking up inspection events.", ex);
            }
        }
    }
    
    private PoleInspectionSummary populateSummary(String authToken, PoleInspectionSummary summary) {

        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setAssetId(summary.getAssetId());
        rparams.setAssetInspectionId(summary.getId());
        rparams.setStatus(ResourceStatus.Released);
        List<ResourceMetadata> resources;
        
        // Populate the pole anomaly report URL, if any.
        rparams.setType(ResourceTypes.PoleAnomalyReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setAnomalyReportDownloadURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // Populate the drone survey sheet URL, if any.
        rparams.setType(ResourceTypes.PoleDroneSurveySheet);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setDroneSurveySheetURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // find the design report, if any.
        rparams.setType(ResourceTypes.PoleDesignReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setDesignReportURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }
        
        // find the analysis report, if any.
        rparams.setType(ResourceTypes.PoleInspectionReport);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setAnalysisReportURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }

        // Get URL for the analysis XML, if any.
        rparams.setType(ResourceTypes.PoleInspectionAnalysisXML);
        resources = resourceService.search(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setAnalysisResultURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }

        // Flight Images
        rparams.setType(ResourceTypes.DroneInspectionImage);
        summary.setFlightImages(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));

        // Ground Images
        rparams.setType(ResourceTypes.ManualInspectionImage);
        summary.setGroundImages(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));

        // Ground Zoomified Images
        rparams.setType(ResourceTypes.ManualInspectionImageZ);
        summary.setGroundImagesZ(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));
        
        // Identified Components Images
        rparams.setType(ResourceTypes.IdentifiedComponents);
        summary.setIdentifiedComponentImages(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));

        // Thermal Images
        rparams.setType(ResourceTypes.Thermal);
        summary.setThermalImages(resourceService.summaryFor(rparams, Comparators.RESOURCE_BY_TIMESTAMP));

        // Other Resources
        rparams.setType(ResourceTypes.Other);
        summary.setOtherResources(resourceService.summaryFor(rparams, null));

        summary.setCriticality(calculateCriticality(summary));
        
        return summary;
    }

    @Override
    public void delete(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        // See if it even exists
        PoleInspection insp = retrieve(authToken, id);
        if (insp != null) {
            authorize(sess, insp);
            {
                // TODO: InspectionEventResources
                
                // TODO: Inspection Events
                
                // Delete any related resources
                ResourceSearchParams params = new ResourceSearchParams();
                params.setAssetInspectionId(insp.getId());
                for (ResourceMetadata rmeta : resourceService.search(authToken, params)) {
                    resourceService.delete(authToken, rmeta.getResourceId());
                }
                
                // Delete any related component inspections
                ComponentInspectionSearchParams ciparams = new ComponentInspectionSearchParams();
                ciparams.setAssetInspectionId(insp.getId());
                for (ComponentInspection ci : componentInspectionService.search(authToken, ciparams)) {
                    componentInspectionService.delete(authToken, ci.getId());
                }
            }
            try {
                // Delete the inspection itself.
                piDao.delete(id);
            } catch (DaoException ex) {
                throw new InternalServerErrorException(String.format("Unable to delete pole inspection %s", id), ex);
            }
        }
    }
}

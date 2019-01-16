package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.ams.util.Comparators;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.domain.ResourceTypes;
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
    
    @Inject private PoleInspectionDao piDao;
    @Inject private ResourceWebServiceImpl resourceService;

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
    }
    
    private PoleInspectionSummary populateSummary(String authToken, PoleInspectionSummary summary) {

        ResourceSearchParams rparams = new ResourceSearchParams();
        rparams.setAssetId(summary.getAssetId());
        rparams.setAssetInspectionId(summary.getId());
        List<ResourceMetadata> resources;
        
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
                // Delete any related resources
                ResourceSearchParams params = new ResourceSearchParams();
                params.setAssetInspectionId(insp.getId());
                for (ResourceMetadata rmeta : resourceService.search(authToken, params)) {
                    resourceService.delete(authToken, rmeta.getResourceId());
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

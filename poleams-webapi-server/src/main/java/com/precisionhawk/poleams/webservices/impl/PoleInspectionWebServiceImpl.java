package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleams.bean.PoleInspectionSummary;
import com.precisionhawk.poleams.bean.ResourceSearchParameters;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleInspectionDao;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.ResourceType;
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
        ensureExists(inspection, "The pole inspection is required.");
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
        ensureExists(inspectionId, "The pole inspection ID is required.");
        try {
            return piDao.retrieve(inspectionId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspection.", ex);
        }
    }

    //TODO: Remove me once we have business rules for criticality.
    private final Random random = new Random();
    
    @Override
    public PoleInspectionSummary retrieveSummary(String authToken, String inspectionId) {
        ensureExists(inspectionId, "The pole inspection ID is required.");
        try {
            PoleInspection inspection = piDao.retrieve(inspectionId);
            if (inspection == null) {
                return null;
            }
            
            return populateSummary(authToken, new PoleInspectionSummary(inspection));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspection data.", ex);
        }
    }

    @Override
    public List<PoleInspection> search(String authToken, PoleInspectionSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            return piDao.search(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole inspections based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, PoleInspection inspection) {
        ensureExists(inspection, "The pole inspection is required.");
        ensureExists(inspection.getId(), "The pole inspection ID is required.");
        try {
            if (!piDao.update(inspection)) {
                throw new NotFoundException(String.format("No pole inspection with ID %s exists.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole inspection.", ex);
        }
    }
    
    
    List<PoleInspectionSummary> retrieveSummary(String authToken, PoleInspectionSearchParameters params) {
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
    
    private void calculateCriticality(PoleInspectionSummary summary) {
        //FIXME: Do actual calculation
        summary.setCriticality(random.nextInt(5 - 1 + 1) + 1); // Randomize so that we can see cool stuff in the UI.
    }
    
    private PoleInspectionSummary populateSummary(String authToken, PoleInspectionSummary summary) {

        ResourceSearchParameters rparams = new ResourceSearchParameters();
        rparams.setPoleId(summary.getPoleId());
        rparams.setPoleInspectionId(summary.getId());
        List<ResourceMetadata> resources;
        
        // find the analysis report, if any.
        rparams.setType(ResourceType.PoleInspectionReport);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setAnalysisReportURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }

        // Get URL for the analysis XML, if any.
        rparams.setType(ResourceType.PoleInspectionAnalysisXML);
        resources = resourceService.query(authToken, rparams);
        if (!resources.isEmpty()) {
            summary.setAnalysisResultURL(resourceService.getResourceDownloadURL(resources.get(0).getResourceId()));
        }

        // Flight Images
        rparams.setType(ResourceType.DroneInspectionImage);
        summary.setFlightImages(resourceService.summaryFor(rparams));

        // Ground Images
        rparams.setType(ResourceType.ManualInspectionImage);
        summary.setGroundImages(resourceService.summaryFor(rparams));

        // Thermal Images
        rparams.setType(ResourceType.Thermal);
        summary.setThermalImages(resourceService.summaryFor(rparams));

        // Other Resources
        rparams.setType(ResourceType.Other);
        summary.setOtherResources(resourceService.summaryFor(rparams));

        calculateCriticality(summary);
        
        return summary;
    }
}

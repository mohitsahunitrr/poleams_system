package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.bean.TransmissionLineInspectionSummary;
import com.precisionhawk.poleams.bean.TransmissionStructureSearchParams;
import com.precisionhawk.poleams.dao.TransmissionLineInspectionDao;
import com.precisionhawk.poleams.domain.TransmissionLineInspection;
import com.precisionhawk.poleams.domain.TransmissionStructure;
import com.precisionhawk.poleams.domain.TransmissionStructureInspection;
import com.precisionhawk.poleams.webservices.TransmissionLineInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureInspectionWebService;
import com.precisionhawk.poleams.webservices.TransmissionStructureWebService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    @Inject
    private TransmissionStructureInspectionWebService transStructureInspSvc;
    
    @Inject
    private TransmissionStructureWebService transStructureSvc;

    @Override
    public TransmissionLineInspection create(String authToken, TransmissionLineInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission line inspection is required.");
        authorize(sess, inspection);
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
            return authorize(sess, validateFound(dao.retrieve(id)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to persist the transmission line inspection data.", ex);
        }
    }

    @Override
    public List<TransmissionLineInspection> search(String authToken, SiteInspectionSearchParams searchParams) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        authorize(sess, searchParams);
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
        ServicesSessionBean sess = lookupSessionBean(authToken);
        TransmissionLineInspection insp = retrieve(authToken, id);
        authorize(sess, insp);
        TransmissionLineInspectionSummary summary = new TransmissionLineInspectionSummary(insp);
        
        // Gather all structures for the site.
        Map<String, TransmissionStructure> structures = new HashMap<>();
        TransmissionStructureSearchParams tsparams = new TransmissionStructureSearchParams();
        tsparams.setSiteId(insp.getSiteId());
        for (TransmissionStructure struct : transStructureSvc.search(authToken, tsparams)) {
            structures.put(struct.getId(), struct);
        }
        LOGGER.debug("Loaded {} transmission structures for summary.", structures.size());
                
        // Find all structure inspections.  Add them and their structures.
        AssetInspectionSearchParams aiparams = new AssetInspectionSearchParams();
        aiparams.setSiteInspectionId(id);
        TransmissionStructure struct;
        List<TransmissionStructureInspection> inspections = transStructureInspSvc.search(authToken, aiparams);
        LOGGER.debug("Loaded {} transmission structure inspections for summary.", structures.size());
        for (TransmissionStructureInspection sinsp : inspections) {
            struct = structures.get(sinsp.getAssetId());
            summary.getStructureInspections().put(
                    struct.getStructureNumber(),
                    transStructureInspSvc.retrieveSummary(authToken, sinsp.getId())
            );
            summary.getStructures().put(
                    struct.getStructureNumber(), struct
            );
        }
        return summary;
    }

    @Override
    public void update(String authToken, TransmissionLineInspection inspection) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(inspection, "Transmission line inspection is required.");
        if (inspection.getId() == null) {
            inspection.setId(UUID.randomUUID().toString());
        }
        try {
            boolean updated = false;
            TransmissionLineInspection i = dao.retrieve(inspection.getId());
            if (i != null) {
                authorize(sess, i);
                updated = dao.update(inspection);
            }
            if (!updated) {
                throw new NotFoundException(String.format("No transmission line inspection with the ID %s found.", inspection.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Unable to persist the transmission line inspection data.", ex);
        }
    }
    
}

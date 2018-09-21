package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.PoleAnalysisImportJobState;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.domain.poledata.PoleData;
import com.precisionhawk.poleams.domain.poledata.PoleEquipment;
import com.precisionhawk.poleams.domain.poledata.PoleSpan;
import com.precisionhawk.poleams.domain.poledata.PowerCircuit;
import com.precisionhawk.poleams.domain.poledata.PrimaryCable;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

/**
 *
 * @author pchapman
 */
@Named
public class PoleWebServiceImpl extends AbstractWebService implements PoleWebService {
    
    @Inject private PoleDao poleDao;

    @Override
    public PoleData create(String authToken, PoleData pole) {
        ensureExists(pole, "The pole is required.");
        if (pole.getId() == null) {
            pole.setId(UUID.randomUUID().toString());
        }
        try {
            if (poleDao.insert(pole)) {
                return pole;
            } else {
                throw new BadRequestException(String.format("The pole %s already exists.", pole.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole.", ex);
        }
    }

    @Override
    public PoleAnalysisImportJobState importAnalysisExcel(String authToken, HttpServletRequest arg1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PoleAnalysisImportJobState importAnalysisXML(String authToken, HttpServletRequest arg1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PoleData retrieve(String authToken, String poleId) {
        ensureExists(poleId, "The pole ID is required.");
        try {
            return poleDao.retrieve(poleId);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole.", ex);
        }
    }

    @Override
    public PoleSummary retrieveSummary(String authToken, String poleId) {
        ensureExists(poleId, "The pole ID is required.");
        
        try {
            PoleData pole = poleDao.retrieve(poleId);
            return summaryFromPoleData(pole);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole.", ex);
        }
    }
    
    List<PoleSummary> retrieveSummaries(String authToken, PoleSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            List<PoleSummary> results = new LinkedList<>();
            for (PoleData p : poleDao.search(params)) {
                results.add(summaryFromPoleData(p));
            }
            return results;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public List<PoleData> search(String authToken, PoleSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            return poleDao.search(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, PoleData pole) {
        ensureExists(pole, "The pole is required.");
        ensureExists(pole.getId(), "The pole ID is required.");
        try {
            if (!poleDao.update(pole)) {
                throw new NotFoundException(String.format("No pole with ID %s exists.", pole.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole.", ex);
        }
    }
    
    private static PoleSummary summaryFromPoleData(PoleData data) {
        if (data == null) {
            return null;
        }
        
        // Equipment Type is of first one found.
        PoleEquipment equip = CollectionsUtilities.firstItemIn(data.getEquipment());
        // Framing and phases are of first circuit.
        PoleSpan span = CollectionsUtilities.firstItemIn(data.getSpans());
        PowerCircuit circuit = span    == null ? null : span.getPowerCircuit();
        PrimaryCable cable   = circuit == null ? null : circuit.getPrimary();

        return new PoleSummary(
                data,
                equip == null ? null : equip.getType(),
                cable == null ? null : cable.getFraming(),
                cable == null ? null : cable.getPhases()
        );
    }
}

package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.poleams.bean.PoleAnalysisImportJobState;
import com.precisionhawk.poleams.bean.PoleSearchParameters;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.poleams.dao.DaoException;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.poledata.CommunicationsCable;
import com.precisionhawk.poleams.domain.poledata.PoleEquipment;
import com.precisionhawk.poleams.domain.poledata.PoleLight;
import com.precisionhawk.poleams.domain.poledata.PoleSpan;
import com.precisionhawk.poleams.domain.poledata.PowerCircuit;
import com.precisionhawk.poleams.domain.poledata.PrimaryCable;
import com.precisionhawk.poleams.domain.poledata.SecondaryCable;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.webservices.PoleWebService;
import java.util.ArrayList;
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
 * @author Philip A. Chapman
 */
@Named
public class PoleWebServiceImpl extends AbstractWebService implements PoleWebService {
    
    @Inject private PoleDao poleDao;

    @Override
    public Pole create(String authToken, Pole pole) {
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
    public Pole retrieve(String authToken, String poleId) {
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
            Pole pole = poleDao.retrieve(poleId);
            return summaryFromPoleData(pole);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole.", ex);
        }
    }
    
    List<PoleSummary> retrieveSummaries(String authToken, PoleSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            List<PoleSummary> results = new LinkedList<>();
            for (Pole p : poleDao.search(params)) {
                results.add(summaryFromPoleData(p));
            }
            return results;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public List<Pole> search(String authToken, PoleSearchParameters params) {
        ensureExists(params, "Search parameters are required.");
        try {
            return poleDao.search(params);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, Pole pole) {
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
    
    private static PoleSummary summaryFromPoleData(Pole data) {
        if (data == null) {
            return null;
        }
        
        PoleSummary summary = new PoleSummary(data);
        summary.setCaTVAttachments(summarizeCommunicationsCables(data, CommunicationsCable.Type.CaTV, 6));

        PoleSpan s = CollectionsUtilities.getItemSafely(data.getSpans(), 0);
        
        summary.setCircuit1SpanLength1(s == null ? null : s.getLength());
        PrimaryCable pcable = (s == null || s.getPowerCircuit() == null) ? null : s.getPowerCircuit().getPrimary();
        summary.setNumberOfPhases(pcable == null ? null : pcable.getPhases());
        summary.setFraming(pcable == null ? null : pcable.getFraming());
        
        SecondaryCable scable = (s == null || s.getPowerCircuit() == null) ? null : s.getPowerCircuit().getSecondary();
        String multiplexType = null;
        String openWireType = null;
        if (scable == null || scable.getMultiplex() == null) {
            // Nothing, they remain null.
        } else if (scable.getMultiplex()) {
            multiplexType = scable.getConductor();
        } else {
            openWireType = scable.getConductor();
        }
        summary.setMultiplexType(multiplexType);
        summary.setOpenWireType(openWireType);
        summary.setOwner("FLorida Light and Power"); //TODO: From org
        summary.setNeutralWireType((s == null || s.getPowerCircuit() == null || s.getPowerCircuit().getNeutral() == null) ? null : s.getPowerCircuit().getNeutral().getConductor());
        summary.setNumberOfOpenWires(scable == null ? null : scable.getWireCount());
        
        s = CollectionsUtilities.getItemSafely(data.getSpans(), 1);
        summary.setCircuit1SpanLength2(s == null ? null : s.getLength());

        s = CollectionsUtilities.getItemSafely(data.getSpans(), 2);
        summary.setPullOff1SpanLength1(s == null ? null : s.getLength());

        s = CollectionsUtilities.getItemSafely(data.getSpans(), 3);
        summary.setPullOff2SpanLength2(s == null ? null : s.getLength());

        summary.setEquipmentQuantity(data.getEquipment().size());
        PoleEquipment equip = CollectionsUtilities.firstItemIn(data.getEquipment());
        summary.setEquipmentType(equip == null ? null : equip.getType());
        summary.setNumberOfCATVAttachments(countAttachments(data, CommunicationsCable.Type.CaTV));
        summary.setNumberOfTelComAttachments(countAttachments(data, CommunicationsCable.Type.Telco));
        summary.setRisers(CollectionsUtilities.copyToMaxSize(data.getRisers(), new LinkedList<>(), 2));
        PoleLight light = CollectionsUtilities.firstItemIn(data.getLights());
        summary.setStreetLight(light == null ? null : light.getType());
        summary.setTelCommAttachments(summarizeCommunicationsCables(data, CommunicationsCable.Type.Telco, 6));
        //TODO: totalSizeCATV
        //TODO: totalSizeTelCom
        return summary;
    }
    
    private static int countAttachments(Pole pole, CommunicationsCable.Type type) {
        int count = 0;
        for (PoleSpan span : pole.getSpans()) {
            for (CommunicationsCable cable : span.getCommunications()) {
                if (cable.getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private static List<CommunicationsCable> summarizeCommunicationsCables(
            Pole pole, CommunicationsCable.Type type, int maxCount
        )
    {
        List<CommunicationsCable> cables = new LinkedList<>();
        for (PoleSpan span : pole.getSpans()) {
            List<CommunicationsCable> list = span.getCommunications();
            for (int i = 0; cables.size() < maxCount && i < list.size(); i++) {
                if (type == list.get(i).getType()) {
                    cables.add(list.get(i));
                }
            }
        }
        return cables;
    }
}

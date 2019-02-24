package com.precisionhawk.poleams.webservices.impl;

import com.precisionhawk.ams.bean.AssetInspectionSearchParams;
import com.precisionhawk.poleams.bean.PoleSearchParams;
import com.precisionhawk.poleams.bean.PoleSummary;
import com.precisionhawk.ams.bean.ResourceSearchParams;
import com.precisionhawk.ams.bean.security.ServicesSessionBean;
import com.precisionhawk.ams.dao.DaoException;
import com.precisionhawk.ams.domain.Organization;
import com.precisionhawk.poleams.dao.PoleDao;
import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.ams.domain.ResourceMetadata;
import com.precisionhawk.poleams.domain.poledata.CommunicationsCable;
import com.precisionhawk.poleams.domain.poledata.PoleEquipment;
import com.precisionhawk.poleams.domain.poledata.PoleLight;
import com.precisionhawk.poleams.domain.poledata.PoleSpan;
import com.precisionhawk.poleams.domain.poledata.PrimaryCable;
import com.precisionhawk.poleams.domain.poledata.SecondaryCable;
import com.precisionhawk.ams.util.CollectionsUtilities;
import com.precisionhawk.ams.webservices.OrganizationWebService;
import com.precisionhawk.ams.webservices.impl.AbstractWebService;
import com.precisionhawk.poleams.webservices.FeederWebService;
import com.precisionhawk.poleams.webservices.PoleInspectionWebService;
import com.precisionhawk.poleams.webservices.PoleWebService;
import com.precisionhawk.ams.webservices.ResourceWebService;
import com.precisionhawk.poleams.domain.Feeder;
import java.util.LinkedList;
import java.util.List;
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
public class PoleWebServiceImpl extends AbstractWebService implements PoleWebService {
    
    @Inject private FeederWebService feederSvc;
    @Inject private OrganizationWebService orgSvc;
    @Inject private PoleDao poleDao;
    @Inject private PoleInspectionWebService poleInspectionSvc;
    @Inject private ResourceWebService resourceSvc;

    @Override
    public Pole create(String authToken, Pole pole) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(pole, "The pole is required.");
        authorize(sess, pole);
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
    public Pole retrieve(String authToken, String poleId) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(poleId, "The pole ID is required.");
        try {
            return authorize(sess, validateFound(poleDao.retrieve(poleId)));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole.", ex);
        }
    }

    @Override
    public PoleSummary retrieveSummary(String authToken, String poleId) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(poleId, "The pole ID is required.");        
        try {
            Pole pole = poleDao.retrieve(poleId);
            authorize(sess, pole);
            return summaryFromPoleData(authToken, pole);
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving pole.", ex);
        }
    }
    
    List<PoleSummary> retrieveSummaries(String authToken, PoleSearchParams params) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(params, "Search parameters are required.");
        try {
            List<PoleSummary> results = new LinkedList<>();
            for (Pole p : authorize(sess, poleDao.search(params))) {
                results.add(summaryFromPoleData(authToken, p));
            }
            return results;
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public List<Pole> search(String authToken, PoleSearchParams params) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(params, "Search parameters are required.");
        authorize(sess, params);
        try {
            return authorize(sess, poleDao.search(params));
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error retrieving poles based on search criteria.", ex);
        }
    }

    @Override
    public void update(String authToken, Pole pole) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(pole, "The pole is required.");
        ensureExists(pole.getId(), "The pole ID is required.");
        try {
            boolean updated = false;
            Pole p = poleDao.retrieve(pole.getId());
            if (p != null) {
                authorize(sess, p);
                updated = poleDao.update(pole);
            }
            if (!updated) {
                throw new NotFoundException(String.format("No pole with ID %s exists.", pole.getId()));
            }
        } catch (DaoException ex) {
            throw new InternalServerErrorException("Error persisting pole.", ex);
        }
    }
    
    private PoleSummary summaryFromPoleData(String authToken, Pole data) {
        if (data == null) {
            return null;
        }
        
        PoleSummary summary = new PoleSummary(data);
        Feeder f = feederSvc.retrieve(authToken, data.getSiteId());
        Organization org = orgSvc.retrieveOrg(f.getOrganizationId());
        summary.setOwner(org.getName());

        summary.setCaTVAttachments(summarizeCommunicationsCables(data, CommunicationsCable.Type.CaTV, 6));

        PoleSpan s = CollectionsUtilities.getItemSafely(data.getSpans(), 0);
        
        summary.setCircuit1SpanLength1(s == null ? null : s.getLength());
        PrimaryCable pcable = (s == null || s.getPowerCircuit() == null) ? null : s.getPowerCircuit().getPrimary();
        summary.setPrimaryWireType(pcable == null ? null : pcable.getConductor());
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
        summary.setNeutralWireType((s == null || s.getPowerCircuit() == null || s.getPowerCircuit().getNeutral() == null) ? null : s.getPowerCircuit().getNeutral().getConductor());
        summary.setNumberOfOpenWires(scable == null ? null : scable.getWireCount());
        
        s = CollectionsUtilities.getItemSafely(data.getSpans(), 1);
        summary.setCircuit1SpanLength2(s == null ? null : s.getLength());

        s = CollectionsUtilities.getItemSafely(data.getSpans(), 2);
        summary.setPullOff1SpanLength1(s == null ? null : s.getLength());
        pcable = (s == null || s.getPowerCircuit() == null) ? null : s.getPowerCircuit().getPrimary();
        summary.setPullOffFraming(pcable == null ? null : pcable.getFraming());

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
        if (!pole.getSpans().isEmpty()) {
            // Only first span is desired as per Irina Denisenko 10/25/2018.
            PoleSpan span = pole.getSpans().get(0);
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
        for (int j = 0; j < pole.getSpans().size(); j++) {
            if (j % 2 == 0) {
                // Only first span is desired as per Irina Denisenko 10/25/2018.
                // Only 1st, 3rd, (odd) spans are desired as per Irina Deniseko 11/05/2018.
                //     Since the index is zero based, this means even idicies.
                PoleSpan span = pole.getSpans().get(j);
                List<CommunicationsCable> list = span.getCommunications();
                for (int i = 0; cables.size() < maxCount && i < list.size(); i++) {
                    if (type == list.get(i).getType()) {
                        cables.add(list.get(i));
                    }
                }
            }
        }
        return cables;
    }

    @Override
    public void delete(String authToken, String id) {
        ServicesSessionBean sess = lookupSessionBean(authToken);
        ensureExists(id, "Pole ID is required");
        // First, see if we even have such a pole
        Pole p = retrieve(authToken, id);
        authorize(sess, p);
        if (p != null) {
            {
                // Delete Inspections, which will delete any shared resources.
                AssetInspectionSearchParams params = new AssetInspectionSearchParams();
                params.setAssetId(id);
                for (PoleInspection insp : poleInspectionSvc.search(authToken, params)) {
                    poleInspectionSvc.delete(authToken, insp.getId());
                }
            }
            {
                // Delete any resources left that are related to the pole.
                ResourceSearchParams params = new ResourceSearchParams();
                params.setAssetId(id);
                for (ResourceMetadata rmeta : resourceSvc.search(authToken, params)) {
                    resourceSvc.delete(authToken, rmeta.getResourceId());
                }
            }
            try {
                // Finally, we can delete the pole.
                poleDao.delete(id);
            } catch (DaoException ex) {
                throw new InternalServerErrorException(String.format("Error deleting the pole %s.\n", id), ex);
            }
        }
    }
}

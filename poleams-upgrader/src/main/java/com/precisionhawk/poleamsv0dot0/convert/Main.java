package com.precisionhawk.poleamsv0dot0.convert;

import com.precisionhawk.ams.bean.SiteInspectionSearchParams;
import com.precisionhawk.ams.bean.WorkOrderSearchParams;
import com.precisionhawk.ams.domain.AssetType;
import com.precisionhawk.ams.domain.SiteInspectionStatus;
import com.precisionhawk.ams.domain.WorkOrder;
import com.precisionhawk.poleams.domain.Feeder;
import com.precisionhawk.poleams.domain.FeederInspection;
import com.precisionhawk.poleams.domain.WorkOrderStatuses;
import com.precisionhawk.poleams.domain.WorkOrderTypes;
import com.precisionhawk.poleamsv0dot0.bean.PoleInspectionSearchParameters;
import com.precisionhawk.poleamsv0dot0.domain.ResourceMetadata;
import com.precisionhawk.poleamsv0dot0.domain.SubStation;
import com.precisionhawk.poleamsv0dot0.util.CollectionsUtilities;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 *
 * @author pchapman
 */
public class Main {
    
    private static final String ARG_SOURCE_CLUSTER = "-sc";
    private static final String ARG_SOURCE_HOST = "-sh";
    private static final String ARG_TARGET_CLUSTER = "-tc";
    private static final String ARG_TARGET_HOST = "-th";

    public static void main(String[] argsArray) {
        ElasticSearchConfigBean sourceESConfig = new ElasticSearchConfigBean();
        ElasticSearchConfigBean targetESConfig = new ElasticSearchConfigBean();
        
        Queue<String> args = new LinkedList<>();
        Collections.addAll(args, argsArray);
        while (!args.isEmpty()) {
            String arg = args.poll();
            switch (arg) {
                case ARG_SOURCE_CLUSTER:
                    if (sourceESConfig.getClusterName() == null) {
                        sourceESConfig.setClusterName(args.poll());
                    } else {
                        System.err.println("Source index (" + ARG_SOURCE_CLUSTER + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_SOURCE_HOST:
                    if (sourceESConfig.getNodeHosts() == null) {
                        sourceESConfig.setNodeHosts(args.poll());
                    } else {
                        System.err.println("Source hosts (" + ARG_SOURCE_HOST + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_TARGET_CLUSTER:
                    if (targetESConfig.getClusterName() == null) {
                        targetESConfig.setClusterName(args.poll());
                    } else {
                        System.err.println("Target index (" + ARG_TARGET_CLUSTER + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                case ARG_TARGET_HOST:
                    if (targetESConfig.getNodeHosts() == null) {
                        targetESConfig.setNodeHosts(args.poll());
                    } else {
                        System.err.println("Target hosts (" + ARG_TARGET_HOST + ") defined more than once.");
                        System.exit(1);
                    }
                    break;
                default:
                    System.err.printf("Invalid argument \"%s\".\n", arg);
                    System.exit(1);
            }
        }
        if (!sourceESConfig.isValid()) {
            System.err.println("Invalid or missing config for source ES cluster.");
            System.exit(1);
        }
        if (!targetESConfig.isValid()) {
            System.err.println("Invalid or missing config for target ES cluster.");
            System.exit(1);
        }
        SourceDAOs sourceDAOs = new SourceDAOs(sourceESConfig);
        TargetDAOs targetDAOs = new TargetDAOs(targetESConfig);
        
        new Main(sourceDAOs, targetDAOs).process();
    }
    
    private final SourceDAOs sourceDAOs;
    private final TargetDAOs targetDAOs;
    private final Tallies tallies;
    private Main(SourceDAOs sourceDAOs, TargetDAOs targetDAOs) {
        this.sourceDAOs = sourceDAOs;
        this.targetDAOs = targetDAOs;
        this.tallies = new Tallies();
    }
    
    private void process() {
        int status = 0;
        try {
            for (SubStation ss : sourceDAOs.getSubStationDao().retrieveAll()) {
                processFeeder(sourceDAOs, targetDAOs, ss);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            status = 1;
        }
        tallies.report();
        System.exit(status);
    }

    private void processFeeder(SourceDAOs sourceDAOs, TargetDAOs targetDAOs, SubStation sSubstation)
        throws Exception
    {
        // Look up feeder in target
        Feeder tFeeder = targetDAOs.getFeederDao().retrieve(sSubstation.getId());
        
        WorkOrder tWorkOrder = null;
        
        // If feeder does not exist, create it.
        if (tFeeder == null) {
            tFeeder = new Feeder();
            tFeeder.setFeederNumber(sSubstation.getFeederNumber());
            tFeeder.setHardeningLevel(sSubstation.getHardeningLevel());
            tFeeder.setId(sSubstation.getId());
            tFeeder.setName(sSubstation.getName());
            tFeeder.setOrganizationId(sSubstation.getOrganizationId());
            tFeeder.setWindZone(sSubstation.getWindZone());
            targetDAOs.getFeederDao().insert(tFeeder);
            tallies.tally(tFeeder.getClass(), true);
        } else {
            tallies.tally(tFeeder.getClass(), false);
            WorkOrderSearchParams wosparams = new WorkOrderSearchParams();
            wosparams.setSiteId(sSubstation.getId());
            tWorkOrder = CollectionsUtilities.firstItemIn(targetDAOs.getWorkOrderDao().search(wosparams));
        }
        
        FeederInspection tFeederInspection = null;

        if (tWorkOrder == null) {
            tWorkOrder = new WorkOrder();
            tWorkOrder.setOrderNumber(UUID.randomUUID().toString().split("-")[0].toUpperCase());
            tWorkOrder.getSiteIds().add(sSubstation.getId());
            tWorkOrder.setStatus(WorkOrderStatuses.Completed);
            tWorkOrder.setType(WorkOrderTypes.DistributionLineInspection);
            targetDAOs.getWorkOrderDao().insert(tWorkOrder);
            tallies.tally(tWorkOrder.getClass(), true);
        } else {
            tallies.tally(tWorkOrder.getClass(), false);
            // Look up feeder inspection
            SiteInspectionSearchParams sisparams = new SiteInspectionSearchParams();
            sisparams.setSiteId(sSubstation.getId());
            sisparams.setOrderNumber(tWorkOrder.getOrderNumber());
            tFeederInspection = CollectionsUtilities.firstItemIn(targetDAOs.getFeederInspectionDao().search(sisparams));
        }
        
        if (tFeederInspection == null) {
            tFeederInspection = new FeederInspection();
            tFeederInspection.setId(UUID.randomUUID().toString());
            tFeederInspection.setOrderNumber(tWorkOrder.getOrderNumber());
            tFeederInspection.setSiteId(sSubstation.getId());
            tFeederInspection.setStatus(new SiteInspectionStatus("Completed"));
            tFeederInspection.setVegitationEncroachmentGoogleEarthURL(sSubstation.getVegitationEncroachmentGoogleEarthURL());
            targetDAOs.getFeederInspectionDao().insert(tFeederInspection);
            tallies.tally(tFeederInspection.getClass(), true);
        } else {
            tallies.tally(tFeederInspection.getClass(), false);
        }
        
        com.precisionhawk.poleamsv0dot0.bean.PoleSearchParameters psparams = new com.precisionhawk.poleamsv0dot0.bean.PoleSearchParameters();
        psparams.setSubStationId(sSubstation.getId());
        for (com.precisionhawk.poleamsv0dot0.domain.Pole sPole : sourceDAOs.getPoleDao().search(psparams)) {
            processPole(sPole, tFeederInspection);
        }
        
        com.precisionhawk.poleamsv0dot0.bean.ResourceSearchParameters rsparams = new com.precisionhawk.poleamsv0dot0.bean.ResourceSearchParameters();
        rsparams.setSubStationId(sSubstation.getId());
        for (com.precisionhawk.poleamsv0dot0.domain.ResourceMetadata sRMeta : sourceDAOs.getResourceMetadataDao().lookup(rsparams)) {
            processResource(sRMeta, tFeederInspection);
        }
    }

    private void processPole(
            com.precisionhawk.poleamsv0dot0.domain.Pole sPole,
            FeederInspection tFeederInspection
        ) throws Exception
    {
        PoleInspectionSearchParameters pisparams = new PoleInspectionSearchParameters();
        pisparams.setPoleId(sPole.getId());
        com.precisionhawk.poleamsv0dot0.domain.PoleInspection sPoleInspection = CollectionsUtilities.firstItemIn(sourceDAOs.getPoleInspectionDao().search(pisparams));
        
        com.precisionhawk.poleams.domain.Pole tPole = targetDAOs.getPoleDao().retrieve(sPole.getId());
        com.precisionhawk.poleams.domain.PoleInspection tPoleInspection = targetDAOs.getPoleInspectionDao().retrieve(sPoleInspection.getId());
        if (tPole == null) {
            tPole = new com.precisionhawk.poleams.domain.Pole();
            copyPoleData(sPole, tPole);
            targetDAOs.getPoleDao().insert(tPole);
            tallies.tally(tPole.getClass(), true);
        } else {
            tallies.tally(tPole.getClass(), false);
        }
        if (tPoleInspection == null) {
            tPoleInspection = new com.precisionhawk.poleams.domain.PoleInspection();
            tPoleInspection.setAccess(sPoleInspection.getAccess());
            tPoleInspection.setAnchorsPass(sPoleInspection.getAnchorsPass());
            tPoleInspection.setAssetId(sPoleInspection.getPoleId());
            tPoleInspection.setBracketsPass(sPoleInspection.getBracketsPass());
            tPoleInspection.setDateOfAnalysis(sPoleInspection.getDateOfAnalysis());
            tPoleInspection.setDownGuysPass(sPoleInspection.getDownGuysPass());
            tPoleInspection.setHorizontalLoadingPercent(sPoleInspection.getHorizontalLoadingPercent());
            tPoleInspection.setId(sPoleInspection.getId());
            tPoleInspection.setInsulatorsPass(sPoleInspection.getInsulatorsPass());
            tPoleInspection.setLatLongDelta(sPoleInspection.getLatLongDelta());
            if (sPoleInspection.getLoadCase() != null) {
                com.precisionhawk.poleams.bean.PoleAnalysisLoadCase lc = new com.precisionhawk.poleams.bean.PoleAnalysisLoadCase();
                lc.setIce(sPoleInspection.getLoadCase().getIce());
                lc.setNescRule(sPoleInspection.getLoadCase().getNescRule());
                lc.setTemperature(sPoleInspection.getLoadCase().getTemperature());
                lc.setWind(sPoleInspection.getLoadCase().getWind());
                tPoleInspection.setLoadCase(lc);
            }
            tPoleInspection.setPassedAnalysis(sPoleInspection.getPassedAnalysis());
            tPoleInspection.setSiteId(sPoleInspection.getSubStationId());
            tPoleInspection.setSiteInspectionId(tFeederInspection.getId());
            tPoleInspection.setVerticalLoadingPercent(sPoleInspection.getVerticalLoadingPercent());
            targetDAOs.getPoleInspectionDao().insert(tPoleInspection);
            tallies.tally(tPoleInspection.getClass(), true);
        } else {
            tallies.tally(tPoleInspection.getClass(), false);
        }
    }

    private void copyPoleData(
            com.precisionhawk.poleamsv0dot0.domain.Pole sPole,
            com.precisionhawk.poleams.domain.Pole tPole
        )
    {
        for (com.precisionhawk.poleamsv0dot0.domain.poledata.PoleAnchor sAnchor : sPole.getAnchors()) {
            com.precisionhawk.poleams.domain.poledata.PoleAnchor tAnchor = new com.precisionhawk.poleams.domain.poledata.PoleAnchor();
            tAnchor.setBearing(sAnchor.getBearing());
            tAnchor.setGuyAssc(com.precisionhawk.poleams.domain.poledata.PoleAnchor.GuyAssc.valueOf(sAnchor.getGuyAssc().name()));
            tAnchor.setLeadLength(sAnchor.getLeadLength());
            tAnchor.setStrandDiameter(sAnchor.getStrandDiameter());
            tPole.getAnchors().add(tAnchor);
        }
        tPole.setDescription(sPole.getDescription());
        for (com.precisionhawk.poleamsv0dot0.domain.poledata.PoleEquipment sEquip : sPole.getEquipment()) {
            com.precisionhawk.poleams.domain.poledata.PoleEquipment tEquip = new com.precisionhawk.poleams.domain.poledata.PoleEquipment();
            tEquip.setDescription(sEquip.getDescription());
            tEquip.setType(tEquip.getType());
            tPole.getEquipment().add(tEquip);
        }
        tPole.setId(sPole.getId());
        tPole.setLength(sPole.getLength());
        for (com.precisionhawk.poleamsv0dot0.domain.poledata.PoleLight sLight : sPole.getLights()) {
            com.precisionhawk.poleams.domain.poledata.PoleLight tLight = new com.precisionhawk.poleams.domain.poledata.PoleLight();
            tLight.setDescription(sLight.getDescription());
            tLight.setType(sLight.getType());
            tPole.getLights().add(tLight);
        }
        tPole.setLocation(copyGeoPoint(sPole.getLocation()));
        tPole.setPoleClass(sPole.getPoleClass());
        for (String sRiser : sPole.getRisers()) {
            tPole.getRisers().add(sRiser);
        }
        tPole.setSiteId(sPole.getSubStationId());
        for (com.precisionhawk.poleamsv0dot0.domain.poledata.PoleSpan sSpan : sPole.getSpans()) {
            com.precisionhawk.poleams.domain.poledata.PoleSpan tSpan = new com.precisionhawk.poleams.domain.poledata.PoleSpan();
            tSpan.setBearing(sSpan.getBearing());
            for (com.precisionhawk.poleamsv0dot0.domain.poledata.CommunicationsCable sCC : sSpan.getCommunications()) {
                com.precisionhawk.poleams.domain.poledata.CommunicationsCable tCC = new com.precisionhawk.poleams.domain.poledata.CommunicationsCable();
                tCC.setDiameter(sCC.getDiameter());
                tCC.setHeight(sCC.getHeight());
                tCC.setType(com.precisionhawk.poleams.domain.poledata.CommunicationsCable.Type.valueOf(sCC.getType().name()));
                tSpan.getCommunications().add(tCC);
            }
            tSpan.setLength(sSpan.getLength());
            if (sSpan.getPowerCircuit() != null) {
                com.precisionhawk.poleams.domain.poledata.PowerCircuit tPC = new com.precisionhawk.poleams.domain.poledata.PowerCircuit();
                if (sSpan.getPowerCircuit().getNeutral() != null) {
                    com.precisionhawk.poleams.domain.poledata.NeutralCable tNC = new com.precisionhawk.poleams.domain.poledata.NeutralCable();
                    tNC.setConductor(sSpan.getPowerCircuit().getNeutral().getConductor());
                    tPC.setNeutral(tNC);
                }
                if (sSpan.getPowerCircuit().getPrimary() != null) {
                    com.precisionhawk.poleams.domain.poledata.PrimaryCable tPCable = new com.precisionhawk.poleams.domain.poledata.PrimaryCable();
                    tPCable.setConductor(sSpan.getPowerCircuit().getPrimary().getConductor());
                    tPCable.setFraming(sSpan.getPowerCircuit().getPrimary().getFraming());
                    tPCable.setPhases(sSpan.getPowerCircuit().getPrimary().getPhases());
                    tPC.setPrimary(tPCable);
                }
                if (sSpan.getPowerCircuit().getSecondary() != null) {
                    com.precisionhawk.poleams.domain.poledata.SecondaryCable tSC = new com.precisionhawk.poleams.domain.poledata.SecondaryCable();
                    tSC.setConductor(sSpan.getPowerCircuit().getSecondary().getConductor());
                    tSC.setMultiplex(sSpan.getPowerCircuit().getSecondary().getMultiplex());
                    tSC.setWireCount(sSpan.getPowerCircuit().getSecondary().getWireCount());
                    tPC.setSecondary(tSC);
                }
                tSpan.setPowerCircuit(tPC);
            }
        }
        tPole.setSwitchNumber(sPole.getSwitchNumber());
        tPole.setTlnCoordinate(sPole.getTlnCoordinate());
        tPole.setType(new AssetType(sPole.getType()));
        tPole.setUtilityId(sPole.getFPLId());
    }
    
    private com.precisionhawk.ams.bean.GeoPoint copyGeoPoint(com.precisionhawk.poleamsv0dot0.bean.GeoPoint sPoint) {
        if (sPoint == null) {
            return null;
        } else {
            com.precisionhawk.ams.bean.GeoPoint tPoint = new com.precisionhawk.ams.bean.GeoPoint();
            tPoint.setAccuracy(sPoint.getAccuracy());
            tPoint.setAltitude(tPoint.getAltitude());
            tPoint.setLatitude(sPoint.getLatitude());
            tPoint.setLongitude(sPoint.getLongitude());
            return tPoint;
        }
    }

    private void processResource(ResourceMetadata sRMeta, FeederInspection tFeederInspection)
        throws Exception
    {
        com.precisionhawk.ams.domain.ResourceMetadata tRMeta = targetDAOs.getResourceMetadataDao().retrieve(sRMeta.getResourceId());
        if (tRMeta == null) {
            tRMeta = new com.precisionhawk.ams.domain.ResourceMetadata();
            tRMeta.setAssetId(sRMeta.getPoleId());
            tRMeta.setAssetInspectionId(sRMeta.getPoleInspectionId());
            tRMeta.setContentType(sRMeta.getContentType());
            tRMeta.setLocation(copyGeoPoint(sRMeta.getLocation()));
            tRMeta.setName(sRMeta.getName());
            tRMeta.setOrderNumber(tFeederInspection.getOrderNumber());
            tRMeta.setResourceId(sRMeta.getResourceId());
            tRMeta.setSiteId(sRMeta.getSubStationId());
            tRMeta.setSiteInspectionId(tFeederInspection.getId());
            if (sRMeta.getSize() != null) {
                tRMeta.setSize(new com.precisionhawk.ams.bean.Dimension(sRMeta.getSize().getWidth(), sRMeta.getSize().getHeight(), sRMeta.getSize().getDepth()));
            }
            tRMeta.setStatus(com.precisionhawk.ams.domain.ResourceStatus.valueOf(sRMeta.getStatus().name()));
            tRMeta.setTimestamp(sRMeta.getTimestamp());
            tRMeta.setType(new com.precisionhawk.ams.domain.ResourceType(sRMeta.getType().name()));
            tRMeta.setZoomifyId(sRMeta.getZoomifyId());
            targetDAOs.getResourceMetadataDao().insert(tRMeta);
            tallies.tally(tRMeta.getClass(), true);
        } else {
            tallies.tally(tRMeta.getClass(), false);
        }
    }
    
    class Tally {
        private int copied;
        private final String name;
        private int total;
        
        Tally(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return String.format("%s subtotal: %d\tcopied: %d", name, total, copied);
        }
    }
    
    class Tallies {
        Map<String, Tally> tallies = new HashMap<>();
        
        void tally(Class clazz, boolean copied) {
            Tally tally = ensure(clazz.getSimpleName());
            tally.total++;
            if (copied) {
                tally.copied++;
            }
        }
        
        private Tally ensure(String name) {
            Tally tally = tallies.get(name);
            if (tally == null) {
                tally = new Tally(name);
                tallies.put(name, tally);
            }
            return tally;
        }
        
        void report() {
            Tally totals = new Tally("Total");
            for (Tally tally : tallies.values()) {
                totals.copied+=tally.copied;
                totals.total+=tally.total;
                System.out.println(tally.toString());
            }
            System.out.println(totals);
        }
    }
}

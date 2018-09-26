package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import com.precisionhawk.poleams.domain.poledata.*;

import java.io.InputStream;
import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Philip A Chapman
 */
public class PoleFormanXMLProcessorTest
{

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final String TEST_XML_PATH = "com/precisionhawk/poleams/processors/poleinspection/PoleForeman.xml";

    @Test
    public void processTest() throws IOException, SAXException {
        Pole pole = new Pole();
        pole.setFPLId("4009449");
        PoleInspection inspection = new PoleInspection();
        InputStream is = getClass().getClassLoader().getResourceAsStream(TEST_XML_PATH);
        PoleForemanXMLProcessor.process(new ProcessListener() {
            @Override
            public void reportFatalError(String message) {
                LOGGER.error(message);
            }

            @Override
            public void reportFatalException(String message, Throwable t) {
                LOGGER.error(message, t);
            }

            @Override
            public void reportFatalException(Exception ex) {
                LOGGER.error("", ex);
            }

            @Override
            public void reportMessage(String message) {
                LOGGER.info(message);
            }

            @Override
            public void reportNonFatalError(String message) {
                LOGGER.warn(message);
            }

            @Override
            public void reportNonFatalException(String message, Throwable t) {
                LOGGER.warn(message, t);
            }
        }, pole, inspection, is);

        // Test Results
        assertNotNull(inspection.getDateOfAnalysis());
        assertEquals(Integer.valueOf(45), pole.getLength());
        assertEquals("6.00 KIP CONC", pole.getPoleClass());
        assertEquals("152 245 002  IIIH", pole.getDescription());
        assertTrue(inspection.getPassedAnalysis());
        assertEquals(Integer.valueOf(99), inspection.getHorizontalLoadingPercent());
        assertEquals(Integer.valueOf(2), inspection.getVerticalLoadingPercent());
        assertNotNull(inspection.getLoadCase());
        assertEquals("250C", inspection.getLoadCase().getNescRule());
        assertEquals("145 mph", inspection.getLoadCase().getWind());
        assertEquals("0.00 in", inspection.getLoadCase().getIce());
        assertEquals("60F", inspection.getLoadCase().getTemperature());
        assertNotNull(pole.getSpans());
        assertEquals(2, pole.getSpans().size());

        // Span 1
        PoleSpan span = pole.getSpans().get(0);
        assertNotNull(span);
        assertEquals("20\u00b0", span.getBearing());
        assertEquals("120'", span.getLength());
        assertNotNull(span.getPowerCircuit());
        assertNotNull(span.getPowerCircuit().getPrimary());
        assertEquals("568 ACAR (15/4)", span.getPowerCircuit().getPrimary().getConductor());
        assertEquals(Integer.valueOf(3), span.getPowerCircuit().getPrimary().getPhases());
        assertEquals("3PH-TAN-AArm-8' 6\" Stl-35KV PT-E-13.0.0 F4 NUET", span.getPowerCircuit().getPrimary().getFraming());
        assertNotNull(span.getPowerCircuit().getNeutral());
        assertEquals("3/0 AAAC (7)", span.getPowerCircuit().getNeutral().getConductor());
        assertNull(span.getPowerCircuit().getSecondary());
        assertNotNull(span.getCommunications());
        assertEquals(1, span.getCommunications().size());
        CommunicationsCable commCable = span.getCommunications().get(0);
        assertNotNull(commCable);
        assertEquals(CommunicationsCable.Type.CaTV, commCable.getType());
        assertEquals(Float.valueOf(1), commCable.getDiameter());
        assertEquals(Float.valueOf(20), commCable.getHeight());

        // Span 2
        span = pole.getSpans().get(1);
        assertNotNull(span);
        assertEquals("200\u00b0", span.getBearing());
        assertEquals("65'", span.getLength());
        assertNotNull(span.getPowerCircuit());
        assertNotNull(span.getPowerCircuit().getPrimary());
        assertEquals("568 ACAR (15/4)", span.getPowerCircuit().getPrimary().getConductor());
        assertEquals(Integer.valueOf(3), span.getPowerCircuit().getPrimary().getPhases());
        assertEquals("*3PH-TAN-AArm-8' 6\" Stl-35KV PT-E-13.0.0 F4 NUET", span.getPowerCircuit().getPrimary().getFraming());
        assertNotNull(span.getPowerCircuit().getNeutral());
        assertEquals("3/0 AAAC (7)", span.getPowerCircuit().getNeutral().getConductor());
        assertEquals("4 AAAC (7)", span.getPowerCircuit().getSecondary().getConductor());
        assertEquals(Integer.valueOf(1), span.getPowerCircuit().getSecondary().getWireCount());
        assertEquals(Boolean.FALSE, span.getPowerCircuit().getSecondary().getMultiplex());
        assertNotNull(span.getCommunications());
        assertEquals(1, span.getCommunications().size());
        commCable = span.getCommunications().get(0);
        assertNotNull(commCable);
        assertEquals(CommunicationsCable.Type.CaTV, commCable.getType());
        assertEquals(Float.valueOf(1), commCable.getDiameter());
        assertEquals(Float.valueOf(20), commCable.getHeight());

        // Equipment
        assertNotNull(pole.getEquipment());
        assertEquals(1, pole.getEquipment().size());
        PoleEquipment equip = pole.getEquipment().get(0);
        assertNotNull(equip);
        assertEquals("3\u00d8 RISER / CO / LA ASSEMBLY", equip.getDescription());
        assertEquals("3\u00d8 RISER / CO / LA ASSEMBLY", equip.getType());

        // Anchors
        assertNotNull(pole.getAnchors());
        assertEquals(0, pole.getAnchors().size());

        // Lights
        assertNotNull(pole.getLights());
        assertEquals(1, pole.getLights().size());
        PoleLight light = pole.getLights().get(0);
        assertEquals("Security Light", light.getType());
        assertEquals("150-400W Flood", light.getDescription());

        // Risers
        assertNotNull(pole.getRisers());
        assertEquals(1, pole.getRisers().size());
        assertEquals("PVC U-GUARD 5\"", pole.getRisers().get(0));

        // Results
        assertNull(inspection.getAnchorsPass());
        assertEquals(Boolean.FALSE, inspection.getBracketsPass());
        assertNull(inspection.getDownGuysPass());
        assertEquals(Boolean.TRUE, inspection.getInsulatorsPass());
    }
}

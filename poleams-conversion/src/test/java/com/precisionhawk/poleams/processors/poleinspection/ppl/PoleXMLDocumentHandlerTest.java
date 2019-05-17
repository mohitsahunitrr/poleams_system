package com.precisionhawk.poleams.processors.poleinspection.ppl;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author pchapman
 */
public class PoleXMLDocumentHandlerTest {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    public static final String TEST_XML_PATH1 = "com/precisionhawk/poleams/processors/poleinspection/ppl/PoleXMLDocument_1.xml";

    private static final int COORDS_XMIN = 0;
    private static final int COORDS_YMIN = 1;
    private static final int COORDS_XMAX = 2;
    private static final int COORDS_YMAX = 3;
    private static final int [][] COORDS_1 = {
        {266, 324, 389, 500},
        {410, 210, 547, 377},
        {527, 131, 633, 282}
    };
    private static final String[] NAMES_1 = {
        "Fuse Switch",
        "Fuse Switch",
        "Fuse Switch"
    };
    
    @Test
    public void xmlProcessTest() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(TEST_XML_PATH1);
            PoleXMLDocumentHandler handler = new PoleXMLDocumentHandler();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));
            
            Observation o;
            String s;
            assertEquals(COORDS_1.length, handler.getObservations().size());
            for (int i = 0; i < COORDS_1.length; i++) {
                o = handler.getObservations().get(i);
                s = String.format("Object %d", i);
                assertEquals(s, NAMES_1[i], o.getName());
                assertEquals(s, COORDS_1[i][COORDS_XMIN], o.getXmin().intValue());
                assertEquals(s, COORDS_1[i][COORDS_XMAX], o.getXmax().intValue());
                assertEquals(s, COORDS_1[i][COORDS_YMIN], o.getYmin().intValue());
                assertEquals(s, COORDS_1[i][COORDS_YMAX], o.getYmax().intValue());
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}

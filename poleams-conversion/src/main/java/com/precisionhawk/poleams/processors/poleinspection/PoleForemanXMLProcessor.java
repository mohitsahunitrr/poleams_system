package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.PoleInspection;
import org.apache.commons.io.IOUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 * @author Philip A. Chapman
 */
class PoleForemanXMLProcessor {

    static boolean process(ProcessListener listener, Pole pole, PoleInspection inspection, File xmlFile) {

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(xmlFile));
            return process(listener, pole, inspection, is);
        } catch (IOException | SAXException e) {
            listener.reportNonFatalException(String.format("Unable to open or parse the XML file at \"%s\"", xmlFile.getAbsolutePath()), e);
            return true;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    static boolean process(ProcessListener listener, Pole pole, PoleInspection inspection, InputStream xmlStream)
            throws IOException, SAXException
    {
        if (pole == null) {
            throw new IllegalArgumentException("Pole is required");
        } else if (inspection == null) {
            throw new IllegalArgumentException("Pole inspection is required.");
        } else {
            PoleForemanDocumentHandler handler = new PoleForemanDocumentHandler(listener, pole, inspection);
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(xmlStream));
            return true;
        }
    }
}

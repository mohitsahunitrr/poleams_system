package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

/**
 *
 * @author Philip A. Chapman
 */
final class PoleDataProcessor {
    
    static boolean process(Environment env, ProcessListener listener, InspectionData data, Pole p, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                if (f.canRead()) {
                    try {
                        ImageFormat format = Imaging.guessFormat(f);
                        if (ImageFormats.UNKNOWN.equals(format)) {
                            if (f.getName().toUpperCase().endsWith(".XML")) {
                                PoleForemanXMLProcessor.process(listener, p, data.getPoleInspectionsByFPLId().get(p.getFPLId()), f);
                            } else {
                                listener.reportMessage(String.format("Unrecognized file \"%s\" has been skipped.", f.getAbsolutePath()));
                            }
                        } else {
                            ImagesProcessor.process(env, listener, data, p, f, format);
                        }
                    } catch (ImageReadException | IOException ex) {
                        listener.reportNonFatalException(String.format("There was an error parsing resource file \"%s\"", f.getAbsolutePath()), ex);

                        return true;
                    }
                } else {
                    listener.reportNonFatalError(String.format("The file \"%s\" is not readable.", f));
                }
            } else {
                listener.reportMessage(String.format("The directory \"%s\" is being ignored.", f));
            }
        }
        return true;
    }    
}

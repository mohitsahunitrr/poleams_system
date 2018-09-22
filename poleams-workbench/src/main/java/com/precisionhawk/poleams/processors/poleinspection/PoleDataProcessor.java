package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.domain.Pole;
import com.precisionhawk.poleams.domain.ResourceType;
import com.precisionhawk.poleams.util.CollectionsUtilities;
import com.precisionhawk.poleams.util.ImageUtilities;
import com.precisionhawk.poleams.webservices.client.Environment;
import java.io.File;
import java.io.IOException;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

/**
 *
 * @author pchapman
 */
final class PoleDataProcessor {
    
    static boolean process(Environment environment, ProcessListener listener, InspectionData data, Pole p, File f) throws ImageReadException, IOException {
        ImageFormat format = Imaging.guessFormat(f);
        if (ImageFormats.UNKNOWN.equals(format)) {
            ImagesProcessor.process(environment, listener, data, p, f, format);
        } else {
            //TODO: Perhaps the XML file?
        }
        
        String ext = CollectionsUtilities.firstItemIn(f.getName().split("\\."));
        ImageUtilities.ImageType imgType = ImageUtilities.ImageType.valueOf(ext.toUpperCase());
    }
    
}

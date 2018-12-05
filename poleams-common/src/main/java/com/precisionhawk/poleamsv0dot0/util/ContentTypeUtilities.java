package com.precisionhawk.poleams.util;

import java.io.File;
import java.io.IOException;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pchapman
 */
public final class ContentTypeUtilities {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTypeUtilities.class);
    
    private ContentTypeUtilities() {}
    
    public static String guessContentType(File f) {
        String contentType = null;
        String fn = f.getName().toUpperCase();
        try {
            ImageFormat format = Imaging.guessFormat(f);
            if (ImageFormats.UNKNOWN.equals(format)) {
                if (fn.endsWith(".ZIP")) {
                    contentType = "application/zip";
                } else if (fn.endsWith(".KML")) {
                    contentType = "application/vnd.google-earth.kml+xml";
                } else if (fn.endsWith(".KMZ")) {
                    contentType = "application/vnd.google-earth.kmz";
                } else if (fn.endsWith(".PDF")) {
                    contentType = "application/pdf";
                } else if (fn.endsWith(".XLSX")) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
            } else {
                ImageInfo info = Imaging.getImageInfo(f);
                ImageMetadata metadata = Imaging.getMetadata(f);
                contentType = info.getMimeType();
            }
        } catch (IOException | ImageReadException ex) {
            LOGGER.error("Error determining content type of file {}", f, ex);
            return null;
        }
        return contentType;
    }
}

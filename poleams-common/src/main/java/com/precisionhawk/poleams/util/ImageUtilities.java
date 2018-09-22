/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.util;

import com.precisionhawk.poleams.bean.Dimension;
import com.precisionhawk.poleams.bean.GeoPoint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public final class ImageUtilities {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtilities.class);
    
    public enum ImageType {
        GIF("image/gif", "GIF"), JPG("image/jpeg", "JPG"), PNG("image/png", "PNG");
        
        private final String contentType;
        private final String iOUtilsIdentifier;
        
        private ImageType(String contentType, String iOUtilsIdentifier) {
            this.contentType = contentType;
            this.iOUtilsIdentifier = iOUtilsIdentifier;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public String getIOUtilsIdentifier() {
            return iOUtilsIdentifier;
        }
        
        public static ImageType fromContentType(String contentType) {
            for (ImageType it : ImageType.values()) {
                if (it.contentType.equals(contentType)) {
                    return it;
                }
            }
            return null;
        }
    }
    
    // Do not instantiate this class.  Resistance is futile.
    private ImageUtilities() {}
    
    private static final double MIN_PERC = .25;
    
    @Deprecated
    public static void crop(String resourceId, InputStream inputStream, OutputStream outputStream, ImageType imageType, Point topLeft, Rectangle rect) throws IOException {
        BufferedImage src = ImageIO.read(inputStream);
        LOGGER.debug(
            "{}: Image width is {}, height is {}", resourceId, src.getWidth(), src.getHeight()
        );
        
        // Figure out percentages and keep the aspect ratio.  We also want to enforce a maximum zoom.
        Double zoomPerc;
        double heightPerc = (double)rect.height / (double)src.getHeight();
        double widthPerc = (double)rect.width / (double)src.getWidth();
        if (heightPerc > widthPerc) {
            zoomPerc = heightPerc;
        } else if (widthPerc > heightPerc) {
            zoomPerc = widthPerc;
        } else {
            zoomPerc = widthPerc;
        }
        if (zoomPerc < MIN_PERC) {
            // Don't zoom past minimum
            zoomPerc = MIN_PERC;
        }
        LOGGER.debug(
            "{}: Requested top left is ({}, {}) width is {}, height is {} width percent is {}, height percent is {}",
            new Object[]{resourceId, rect.x, rect.y, rect.width, rect.height, widthPerc, heightPerc}
        );
        
        // Figure out our new boundaries making sure we center on the old bounds as much as possible.
        int height = (int)(src.getHeight() * zoomPerc);
        int width = (int)(src.getWidth() * zoomPerc);
        // Calculate center of original
        int centerX = rect.x + (int)(rect.width/2);
        int centerY = rect.y + (int)(rect.height/2);
        // Adjust for new zoom ratio
        int minX = centerX - (int)(width/2);
        int minY = centerY - (int)(height/2);
        if (minX < 0) {
            minX = 0;
        } else if (minX + width > src.getWidth()) {
            minX = src.getWidth() - width;
        }
        if (minY < 0) {
            minY = 0;
        } else if (minY + height > src.getHeight()) {
            minY = src.getHeight() - height;
        }
        rect.height = height;
        rect.width = width;
        rect.x = minX;
        rect.y = minY;
        LOGGER.debug(
            "{}: Calculated top left is ({}, {}) width is {}, height is {}, scale percent is {}",
            new Object[]{resourceId, rect.x, rect.y, rect.width, rect.height, zoomPerc}
        );

        // Crop the image
        BufferedImage dest = src.getSubimage(minX, minY, width, height);
        ImageIO.write(dest, imageType.getIOUtilsIdentifier(), outputStream);
    }

    @Deprecated
    public static void scale(InputStream inputStream, OutputStream outputStream, ImageType imageType, int targetWidth, int targetHeight) throws IOException {
        BufferedImage bi = ImageIO.read(inputStream);
        bi = scale(bi, targetWidth, targetHeight);
        ImageIO.write(bi, imageType.getIOUtilsIdentifier(), outputStream);
    }
    
    @Deprecated
    public static BufferedImage scale(BufferedImage sbi, int targetWidth, int targetHeight) {
        double fHeight = targetHeight / sbi.getHeight();
        double fWidth = targetWidth / sbi.getWidth();
        BufferedImage dbi = new BufferedImage(targetWidth, targetHeight, sbi.getType());
        Graphics2D g = dbi.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance(fWidth, fHeight);
        g.drawRenderedImage(sbi, at);
        return dbi;
    }
    
    public static GeoPoint getLocation(TiffImageMetadata metadata) throws ImageReadException, IOException {
        TiffImageMetadata.GPSInfo gpsInfo = metadata.getGPS();
        if (gpsInfo != null) {
            GeoPoint p = new GeoPoint();
            p.setLatitude(gpsInfo.getLatitudeAsDegreesNorth());
            p.setLongitude(gpsInfo.getLongitudeAsDegreesEast());
            TiffField field = metadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
            if (field != null) {
                p.setAltitude(field.getDoubleValue());
            }
            return p;
        } else {
            return null;
        }
    }

    public static Dimension getSize(File f) throws ImageReadException, IOException {
        ImageInfo iinfo = Imaging.getImageInfo(f);
        Dimension d = new Dimension();
        d.setHeight(Double.valueOf(iinfo.getHeight()));
        d.setWidth(Double.valueOf(iinfo.getWidth()));
        return d;
    }
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSZ");
    public static ZonedDateTime getTimestamp(TiffImageMetadata metadata) throws ImageReadException {
        // TiffTagConstants.TIFF_TAG_DATE_TIME
        TiffField field = metadata.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
        if (field != null) {
            String s = field.getStringValue();
            if (s != null && (!s.isEmpty())) {
                return ZonedDateTime.parse(s, DATE_TIME_FORMATTER);
            }
        }
        return null;
    }
}

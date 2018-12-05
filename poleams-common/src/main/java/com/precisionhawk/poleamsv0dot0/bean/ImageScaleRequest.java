/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Schema(description="A bean containing criteria for scaling an image.")
public class ImageScaleRequest extends Dimension {
    @Schema(description="A enumeration describing how an image is to be scaled.")
    public enum ScaleOperation {
        @Schema(description="Scale the image to fit within the bounds of the height and width given, maintaining proportions.")
        ScaleToFit,
        @Schema(description="Scale the image to fit the given height.  Width will be adjusted accordingly to maintain proportions.")
        ScaleToHeight,
        @Schema(description="Scale the image to fit the given widths and heights exactly regardless of the proportions of the original image.")
        ScaleToSize,
        @Schema(description="Scale the image to fit the given width.  Height will be adjusted accordingly to maintain proportions.")
        ScaleToWidth
    };
    
    public enum ContentType {
        GIF,
        JPEG,
        PNG
    };
    
    private ContentType resultType;
    private ScaleOperation scaleOperation;

    public ContentType getResultType() {
        return resultType;
    }
    public void setResultType(ContentType resultType) {
        this.resultType = resultType;
    }

    public ScaleOperation getScaleOperation() {
        return scaleOperation;
    }
    public void setScaleOperation(ScaleOperation scaleOperation) {
        this.scaleOperation = scaleOperation;
    }
}

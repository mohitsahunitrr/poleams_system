package com.precisionhawk.poleams.processors.poleinspection.ppl;

import java.text.NumberFormat;

/**
 *
 * @author pchapman
 */
class RegexBuilder {
    
    private static final String EXT_JPG = ".((JPG)|(jpg))";
    private static final String EXT_XML = ".((XML)|(xml))";
    private static final String IMG_RGB = "rgb";
    private static final String IMG_THERM = "thermal";
    private static final String IMG_BOTH = "((" + IMG_RGB + ")|(" + IMG_THERM + "))";    
    
    private RegexBuilder() {}
    
    static String imageFileNameRegex(String poleNum, String imageNum) {
        return buildRegex(poleNum, imageNum, IMG_BOTH, EXT_JPG);
    }
    
    static String rgbImageFileNameRegex(String poleNum, String imageNum) {
        return buildRegex(poleNum, imageNum, IMG_RGB, EXT_JPG);
    }
    
    static String thermalImageFileNameRegex(String poleNum, String imageNum) {
        return buildRegex(poleNum, imageNum, IMG_THERM, EXT_JPG);
    }
    
    static String xmlFileNameRegex(String poleNum, String imageNum) {
        return buildRegex(poleNum, imageNum, IMG_RGB, EXT_XML);
    }
    
    private static String buildRegex(String poleNum, String imageNum, String imgTypePart, String extensionPart) {
        int in;
        int pn;
        String imageNumber;
        String poleNumber;
        try {
            in = Integer.valueOf(imageNum);
            NumberFormat f = NumberFormat.getIntegerInstance();
            f.setMaximumIntegerDigits(2);
            f.setMinimumIntegerDigits(2);
            imageNumber = f.format(in).replace(",", "");
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(String.format("Invalid image number %s", imageNum));
        }
        try {
            pn = Integer.valueOf(poleNum);
            NumberFormat f = NumberFormat.getIntegerInstance();
            f.setMaximumIntegerDigits(4);
            f.setMinimumIntegerDigits(4);
            poleNumber = f.format(pn).replace(",", "");
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(String.format("Invalid pole number %s", imageNum));
        }
        String regex = String.format(".*_Pole%s_%s%s%s", poleNumber, imgTypePart, imageNumber, extensionPart);
        return regex;
    }
}

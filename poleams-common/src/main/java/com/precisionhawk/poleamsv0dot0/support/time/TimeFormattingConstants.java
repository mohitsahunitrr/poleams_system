package com.precisionhawk.poleamsv0dot0.support.time;

import java.time.format.DateTimeFormatter;

/**
 *
 * @author Philip A. Chapman
 */
public interface TimeFormattingConstants {
    final String DATE_FORMAT = "yyyyMMdd";
    final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    final String DATE_TIME_FORMAT = "yyyyMMdd'T'HHmmss.SSSZ";
    final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    final String TIME_FORMAT = "'T'HHmmss.SSSZ";
    final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);

}

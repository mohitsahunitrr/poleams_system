package com.precisionhawk.poleams.reporting;

/**
 *
 * @author pchapman
 */
public class ReportingException extends Exception {
    
    public ReportingException(String message) {
        super(message);
    }

    public ReportingException(String message, Throwable cause) {
        super(message, cause);
    }
 
    public static ReportingException build(Throwable cause, String format, Object ... args) {
        String msg = String.format(format, args);
        return new ReportingException(msg, cause);
    }
}

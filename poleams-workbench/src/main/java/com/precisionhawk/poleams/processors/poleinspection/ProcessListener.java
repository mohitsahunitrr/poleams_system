package com.precisionhawk.poleams.processors.poleinspection;

/**
 *
 * @author pchapman
 */
public interface ProcessListener {

    public void setStatus(ProcessStatus processStatus);

    public void reportFatalError(String message);

    public void reportFatalException(String message, Exception ioe);

    public void reportFatalException(Exception ioe);

    public void reportMessage(String format);

    public void reportNonFatalError(String message);
    
}

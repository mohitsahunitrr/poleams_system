package com.precisionhawk.poleams.processors.poleinspection;

/**
 *
 * @author pchapman
 */
public interface ProcessListener {

    public void setStatus(ProcessStatus processStatus);

    public void reportFatalError(String message);

    public void reportFatalException(String message, Throwable t);

    public void reportFatalException(Exception ex);

    public void reportMessage(String message);

    public void reportNonFatalError(String message);
    
    public void reportNonFatalException(String message, Throwable t);

}

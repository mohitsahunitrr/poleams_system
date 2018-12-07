package com.precisionhawk.poleams.processors.poleinspection;

import com.precisionhawk.poleams.processors.ProcessListener;

/**
 *
 * @author pchapman
 */
public interface ImportProcessListener extends ProcessListener {

    public void setStatus(ImportProcessStatus processStatus);
}

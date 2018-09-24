package com.precisionhawk.poleams.bean;

/**
 *
 * @author Philip A. Chapman
 */
public class PoleAnalysisImportJobState extends AsyncJobState
{
    @Override
    public AsyncJobState copy() {
        PoleAnalysisImportJobState copy = new PoleAnalysisImportJobState();
        super.copyInto(copy);
        return copy;
    }
}

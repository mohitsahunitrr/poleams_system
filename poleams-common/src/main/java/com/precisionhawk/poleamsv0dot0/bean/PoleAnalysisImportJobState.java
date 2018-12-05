package com.precisionhawk.poleamsv0dot0.bean;

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

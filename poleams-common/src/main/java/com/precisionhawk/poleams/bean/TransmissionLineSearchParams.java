package com.precisionhawk.poleams.bean;

import com.precisionhawk.ams.bean.SiteSearchParams;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="Search parameters for transmission lines.")
public class TransmissionLineSearchParams extends SiteSearchParams {
    @Schema(description="The unique number associated with the line by the utility.")
    private String lineNumber;
    public String getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean hasCriteria() {
        return super.hasCriteria() || hasValue(lineNumber);
    }
    
    public TransmissionLineSearchParams() {}
    
    public TransmissionLineSearchParams(SiteSearchParams params) {
        setName(params.getName());
        setOrganizationId(params.getOrganizationId());
    }
}

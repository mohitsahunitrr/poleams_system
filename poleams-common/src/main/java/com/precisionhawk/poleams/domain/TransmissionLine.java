package com.precisionhawk.poleams.domain;

import com.precisionhawk.ams.domain.Site;
import io.swagger.oas.annotations.media.Schema;

/**
 *
 * @author pchapman
 */
@Schema(description="The line of transmission structures that carries high voltage electricity over long distances.")
public class TransmissionLine extends Site {
    
    @Schema(description="The unique number associated with the line by the utility.")
    private String lineNumber;
    public String getLineNumber() {
        return lineNumber;
    }
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }
    
}

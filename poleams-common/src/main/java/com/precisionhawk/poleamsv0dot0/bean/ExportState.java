/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.bean;

import io.swagger.oas.annotations.media.Schema;

/**
 * The state of an asynchronous job which compiles data to be exported.
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Schema(description="The state of an asynchronous job which compiles data to be exported.")
public class ExportState extends AsyncJobState {

    @Schema(description="The number of objects to be exported.")
    private Integer objectCount = 0;
    @Schema(description="The number of objects prepared for export.")
    private Integer objectsPrepared = 0;
    
    public Integer getObjectCount() {
        return objectCount;
    }
    public void setObjectCount(Integer resourceCount) {
        this.objectCount = resourceCount;
    }

    public Integer getObjectsPrepared() {
        return objectsPrepared;
    }
    public void setObjectsPrepared(Integer resourcesPrepared) {
        this.objectsPrepared = resourcesPrepared;
    }
    
    public void incrementObjectsPrepared() {
        objectsPrepared++;
    }
    
    public ExportState copy() {
        ExportState state = new ExportState();
        super.copyInto(state);
        state.setObjectCount(objectCount);
        state.setObjectsPrepared(objectsPrepared);
        return state;
    }
}

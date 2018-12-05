/*
 * All rights reserved.
 */

package com.precisionhawk.poleamsv0dot0.bean;

import io.swagger.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * The state of an asynchronous job.
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Schema(description="The state of an asynchronous job.")
public abstract class AsyncJobState {

    public enum Status {
        Processing, Completed, Error
    }

    @Schema(description="When the job ended.")
    protected ZonedDateTime endTime;
    @Schema(description="When the result of the job, if any, expires and will no longer be available for download.")
    protected ZonedDateTime expireTime;
    @Schema(description="When the job started.")
    protected ZonedDateTime startTime;
    @Schema(description="When status of the job.")
    protected Status status;
    @Schema(description="When unique ID of the job.")
    protected String uuid;

    public AsyncJobState() {
        uuid = UUID.randomUUID().toString();
        status = Status.Processing;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public ZonedDateTime getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(ZonedDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public abstract AsyncJobState copy();
    
    protected void copyInto(AsyncJobState state) {
        state.setEndTime(endTime);
        state.setExpireTime(expireTime);
        state.setStartTime(startTime);
        state.setStatus(status);
        state.setUuid(uuid);
    }
}

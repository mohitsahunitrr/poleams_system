/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.processor.bean;

import java.time.ZonedDateTime;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
public class JobInfo {
    
    public JobInfo() {}

    private String jobName;
    public String getJobName() {
        return jobName;
    }
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    private ZonedDateTime lastStart;
    public synchronized ZonedDateTime getLastStart() {
        return lastStart;
    }
    public synchronized void setLastStart(ZonedDateTime lastStart) {
        this.lastStart = lastStart;
    }

    private ZonedDateTime lastFinish;
    public synchronized ZonedDateTime getLastFinish() {
        return lastFinish;
    }
    public synchronized void setLastFinish(ZonedDateTime lastFinish) {
        this.lastFinish = lastFinish;
    }
    
    private Integer lastProcessedCount = 0;
    public synchronized Integer getLastProcessedCount() {
        return lastProcessedCount;
    }
    public synchronized void setLastProcessedCount(Integer lastProcessedCount) {
        this.lastProcessedCount = lastProcessedCount;
    }
    
    private String statusMessage;
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public synchronized JobInfo copy() {
        JobInfo info = new JobInfo();
        copyInto(info);
        return info;
    }
    
    public synchronized void copyInto(JobInfo info) {
        info.jobName = this.jobName;
        info.lastFinish = this.lastFinish;
        info.lastProcessedCount = this.lastProcessedCount;
        info.lastStart = this.lastStart;
    }
    
    public synchronized void incrementProcessedCount(int i) {
        if (lastProcessedCount == null) {
            lastProcessedCount = i;
        } else {
            lastProcessedCount = lastProcessedCount + i;
        }
    }
    
    public synchronized void update(ZonedDateTime lastStart, ZonedDateTime lastFinish, Integer lastProcessedCount, String statusMessage) {
        this.lastFinish = lastFinish;
        this.lastStart = lastStart;
        this.lastProcessedCount = lastProcessedCount;
        this.statusMessage = statusMessage;
    }
}

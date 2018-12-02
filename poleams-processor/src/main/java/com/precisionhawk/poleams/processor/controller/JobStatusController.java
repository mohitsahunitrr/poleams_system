/*
 * All rights reserved.
 */

package com.precisionhawk.poleams.processor.controller;

import com.precisionhawk.poleams.processor.bean.JobInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author <a href="mailto:pchapman@pcsw.us">Philip A. Chapman</a>
 */
@Controller
@RequestMapping("/status")
public class JobStatusController implements MAVConstants {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final ZoneId tz = ZoneId.of("US/Pacific");
    
    @Value("${zoomify.cron}")
    private String zoomifyCron;
    @Inject @Named(QUALIFIER_ZOOMIFY_JOB_INFO)
    private JobInfo zoomifyJobInfo;
    
    @Value("${application.name}")
    private String appName;
    @Value("${application.version}")
    private String appVersion;
    
    @RequestMapping(value="/", method=RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> showStatuses()
    {
        return showAppStatus();
    }
    
    @RequestMapping(method=RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> showAppStatus() {
        Map<String, Object> status = new HashMap();
        Map<String, Object> map = new HashMap();
        map.put("name", appName);
        map.put("version", appVersion);
        status.put("application", map);
        List<ExtJobInfo> jobInfo = new ArrayList<>();
        jobInfo.add(new ExtJobInfo(tz, zoomifyJobInfo, zoomifyCron));
        status.put("jobs", jobInfo);
        return status;
    }
    
    private ZonedDateTime convertTimeZone(ZonedDateTime local) {
        if (local == null) {
            return null;
        }
        return local.withZoneSameInstant(tz);
    }
}

class ExtJobInfo {
    
    ExtJobInfo(ZoneId tz, JobInfo original, String cron) {
        JobInfo copy = original.copy();
        this.cron = cron;
        this.jobName = copy.getJobName();
        this.lastFinish = formatTimestamp(copy.getLastFinish(), tz);
        this.lastProcessedCount = copy.getLastProcessedCount();
        this.lastStart = formatTimestamp(copy.getLastStart(), tz);
        this.statusMessage = copy.getStatusMessage();
    }
    
    private String formatTimestamp(ZonedDateTime timestamp, ZoneId tz) {
        if (timestamp == null) {
            return null;
        }
        if (tz != null) {
            timestamp = timestamp.withZoneSameInstant(tz);
        }
        return timestamp.format(DateTimeFormatter.ISO_INSTANT);
    }
    
    private String cron;
    public String getCron() {
        return cron;
    }
    public void setCron(String cron) {
        this.cron = cron;
    }

    private String jobName;
    public String getJobName() {
        return jobName;
    }
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    private String lastStart;
    public String getLastStart() {
        return lastStart;
    }
    public void setLastStart(String lastStart) {
        this.lastStart = lastStart;
    }

    private String lastFinish;
    public String getLastFinish() {
        return lastFinish;
    }
    public void setLastFinish(String lastFinish) {
        this.lastFinish = lastFinish;
    }
    
    private Integer lastProcessedCount = 0;
    public Integer getLastProcessedCount() {
        return lastProcessedCount;
    }
    public void setLastProcessedCount(Integer lastProcessedCount) {
        this.lastProcessedCount = lastProcessedCount;
    }
    
    private String statusMessage;
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
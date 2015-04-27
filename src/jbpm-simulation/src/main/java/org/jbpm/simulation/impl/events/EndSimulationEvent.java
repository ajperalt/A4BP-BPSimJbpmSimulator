package org.jbpm.simulation.impl.events;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

public class EndSimulationEvent extends GenericSimulationEvent {

    private long processDuration;
    private String activityName;
    private String activityId;
    
    private String processName;
    private String processVersion;
    
    public EndSimulationEvent(String processId, long processInstanceId, long startTime, long endTime, long proceesStartTime,
            String activityId, String activityName, String processName, String processVersion) {
        super(processId, processInstanceId, startTime, endTime, "endEvent");
        this.setProcessDuration(endTime - proceesStartTime);
        this.activityId = activityId;
        this.activityName = activityName;
        this.processName = processName;
        this.processVersion = processVersion;
    }

    public String getActivityName() {
        if (StringUtils.isNotEmpty(this.activityName)) {
            return this.activityName;
        }

        return this.activityId;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public long getProcessDuration() {
        return processDuration;
    }

    public void setProcessDuration(long processDuration) {
        this.processDuration = processDuration;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    @Override
    public String toString() {
        
        return "EndSimulationEvent[process=" + processId + ", instance=" + processInstanceId + ", activity=" + activityName + ", endTime=" + new Date(endTime) + ", processDuration=" + processDuration/1000+" seconds]";
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessVersion() {
        return processVersion;
    }

    public void setProcessVersion(String processVersion) {
        this.processVersion = processVersion;
    }
}

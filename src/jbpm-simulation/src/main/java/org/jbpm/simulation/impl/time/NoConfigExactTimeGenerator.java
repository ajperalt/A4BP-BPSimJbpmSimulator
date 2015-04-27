package org.jbpm.simulation.impl.time;

import java.util.concurrent.TimeUnit;

import org.jbpm.simulation.TimeGenerator;

public class NoConfigExactTimeGenerator implements TimeGenerator {

    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    
    public NoConfigExactTimeGenerator() {
        
    }

    public long generateTime() {
        TimeUnit tu = TimeUnit.MINUTES;
        long duration = 1l;
        return timeUnit.convert(duration, tu);
    }

}

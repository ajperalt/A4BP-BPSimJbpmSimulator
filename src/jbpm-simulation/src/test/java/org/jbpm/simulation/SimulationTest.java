package org.jbpm.simulation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.core.time.SessionPseudoClock;
import org.jbpm.simulation.converter.SimulationFilterPathFormatConverter;
import org.jbpm.simulation.helper.HardCodedSimulationDataProvider;
import org.jbpm.simulation.helper.TestUtils;
import org.jbpm.simulation.impl.SimulationPath;
import org.junit.Test;
import org.kie.internal.runtime.StatefulKnowledgeSession;

//@Ignore
public class SimulationTest {
    
    @Test
    public void testParallelGatewayProcessSimulation() {
        PathFinder finder = PathFinderFactory.getInstance(this.getClass().getResourceAsStream("/BPMN2-ParallelSplit.bpmn2"));
        
        List<SimulationPath> paths = finder.findPaths(new SimulationFilterPathFormatConverter());
        
        SimulationContext context = SimulationContextFactory.newContext(new HardCodedSimulationDataProvider());
        
        for (SimulationPath path : paths) {
            
            context.setCurrentPath(path);
            StatefulKnowledgeSession session = TestUtils.createSession("BPMN2-ParallelSplit.bpmn2");
            context.setClock((SessionPseudoClock) session.getSessionClock());
            // set start date to current time
            context.getClock().advanceTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            
            session.startProcess("com.sample.test");
            System.out.println("#####################################");
        }
    }
    
    @Test
    public void testExclusiveGatewayProcessSimulation() {
        PathFinder finder = PathFinderFactory.getInstance(this.getClass().getResourceAsStream("/BPMN2-ExclusiveSplit.bpmn2"));
        
        List<SimulationPath> paths = finder.findPaths(new SimulationFilterPathFormatConverter());
        SimulationContext context = SimulationContextFactory.newContext(new HardCodedSimulationDataProvider());
        
        
        for (SimulationPath path : paths) {
            
            context.setCurrentPath(path);
            StatefulKnowledgeSession session = TestUtils.createSession("BPMN2-ExclusiveSplit.bpmn2");
            
            context.setClock((SessionPseudoClock) session.getSessionClock());
            // set start date to current time
            context.getClock().advanceTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            
            session.startProcess("com.sample.test");
            System.out.println("#####################################");
        }
    }
    
    @Test
    public void testUserTaskProcessSimulation() {
        PathFinder finder = PathFinderFactory.getInstance(this.getClass().getResourceAsStream("/BPMN2-UserTask.bpmn2"));
        
        List<SimulationPath> paths = finder.findPaths(new SimulationFilterPathFormatConverter());
        SimulationContext context = SimulationContextFactory.newContext(new HardCodedSimulationDataProvider());
        
        
        for (SimulationPath path : paths) {
            
            context.setCurrentPath(path);
            StatefulKnowledgeSession session = TestUtils.createSession("BPMN2-UserTask.bpmn2");
            
            context.setClock((SessionPseudoClock) session.getSessionClock());
            // set start date to current time
            context.getClock().advanceTime(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            
            session.startProcess("UserTask");
            System.out.println("#####################################");
        }
    }
    
}

package org.jbpm.simulation.handler;

import java.util.List;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.ExclusiveGateway;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.bpmn2.InclusiveGateway;
import org.eclipse.bpmn2.LinkEventDefinition;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.jbpm.simulation.PathContext;
import org.jbpm.simulation.PathContextManager;

public class ThrowEventElementHandler extends DefaultElementHandler {

    @Override
    public boolean handle(FlowElement element, PathContextManager manager) {
        List<EventDefinition> throwDefinitions = getEventDefinitions(element);
    
        if (throwDefinitions != null && throwDefinitions.size() > 0) {
            for (EventDefinition def : throwDefinitions) {
                String key = "";
            	BaseElement be = null;
                if (def instanceof SignalEventDefinition) {
                    key = ((SignalEventDefinition) def).getSignalRef();
                } else if (def instanceof MessageEventDefinition) {
                	be = ((MessageEventDefinition) def).getMessageRef();
                	if( be != null) key = be.getId(); 
                } else if (def instanceof LinkEventDefinition) {
                    key = ((LinkEventDefinition) def).getName();
                } else if (def instanceof CompensateEventDefinition) {
                	be = ((CompensateEventDefinition) def).getActivityRef();
                	if( be != null) key = be.getId(); 
                } else if (def instanceof ErrorEventDefinition) {
                    key = ((ErrorEventDefinition) def)
                            .getErrorRef().getId();
                }

                FlowElement catchEvent = manager.getCatchingEvents().get(key);
                if (catchEvent != null && hasPathToCatchEvent(catchEvent)) {
                    PathContext context = manager.getContextFromStack();
                    boolean canBeFinished = context.isCanBeFinished();
                    context.setCanBeFinishedNoIncrement(false);
                    super.handle(catchEvent, manager);
                    context.setCanBeFinishedNoIncrement(canBeFinished);
                }
            }

        }
        return super.handle(element, manager);

    }

    protected boolean hasPathToCatchEvent(FlowElement catchEvent) {
        List<SequenceFlow> incoming = ((CatchEvent)catchEvent).getIncoming();

        boolean hasSplitActivity = findSplitActivity(incoming);
        // if there is split activity before catch event consider it's not reachable
        // as simulation cannot determine if it can be reached or not so pessimistically
        // assume it cannot
        if (hasSplitActivity) {
            return false;
        }

        return true;

    }

    protected boolean findSplitActivity(List<SequenceFlow> incoming) {
        if (incoming == null) {
            return false;
        }

        for (SequenceFlow sq : incoming) {
            FlowNode source = sq.getSourceRef();
            if (source instanceof ExclusiveGateway || source instanceof InclusiveGateway) {
                  if (((Gateway) source).getGatewayDirection().equals(GatewayDirection.DIVERGING)) {
                      return true;
                  }
            }
            boolean found = findSplitActivity(source.getIncoming());
            if (found) {
                return found;
            }
        }

        return false;
    }

}


package com.zygon.mmesh.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.Message;
import java.util.Collection;
import java.util.Map;

/**
 * A simple message router.  For now it finds the appropriate queue to put
 * the outgoing message into.
 * 
 * It lives in the core package for now. We need to get the destination queue by
 * Identifier but I don't want to have a global Identifier lookup table.
 *
 * @author zygon
 */
public class Router {
    
    private final Map<Identifier, MessageQueue> destinations = Maps.newHashMap();
    private final Identifier sourceId;

    public Router(Identifier sourceId) {
        this.sourceId = sourceId;
    }
    
    public void send(Identifier originalSource, Message message) {
        
        // different routing rules for the types of message
        switch (message.getType()) {
            case PREDICTION:
                // Put the message in the correct outgoing queue. don't
                // change the source/dest information.
                
                // Activation messages go to all neighbors so it's easy to splash
                // them around.  Prediction messages, however, need to be routed 
                // properly to their destinations.
                
                Identifier selectedId = null;
                double selectedDistance = Double.MAX_VALUE;
                MessageQueue selectedQueue = null;
                
                for (Map.Entry<Identifier, MessageQueue> dest : this.destinations.entrySet()) {
                    
                    Identifier destId = dest.getKey();
                    double distanceAway = destId.getDistance(message.getDestination());
                    MessageQueue destQueue = dest.getValue();
                    
                    if (selectedId == null) {
                        selectedId = destId;
                        selectedDistance = distanceAway;
                        selectedQueue = destQueue;
                    } else {
//                        System.out.printf("%s comparedTo %s = [%d]\n", destId, message.getDestination(), destId.compareTo(message.getDestination()));
                        
                        if (distanceAway < selectedDistance) {
                            selectedId = destId;
                            selectedDistance = distanceAway;
                            selectedQueue = destQueue;
                            
                            if (distanceAway == 0.0) {
                                // This should be the correct one - just break
                                break;
                            }
                        }
                    }
                }
                
                selectedQueue.put(message);
                break;
	    case RESIDUAL:
		for (Map.Entry<Identifier, MessageQueue> dest : this.destinations.entrySet()) {
		    // don't send back to the original location
		    if (!dest.getKey().equals(originalSource)) {
			Message msg = message.setDestination(dest.getKey());
			dest.getValue().put(msg);
		    }
		}
		break;
                
            default:
                throw new UnsupportedOperationException("Should not be routing: " + message.getType().name());
        }
    }
    
    /*pkg*/ final void setDestinations(Collection<Cell> destinations) {
        Preconditions.checkArgument(destinations != null && !destinations.isEmpty());
        
        this.destinations.clear();
        
        for (Cell cell : destinations) {
            this.destinations.put(cell.getIdentifier(), cell.getInputQueue());
        }
    }
}

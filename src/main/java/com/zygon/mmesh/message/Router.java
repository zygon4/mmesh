
package com.zygon.mmesh.message;

import com.zygon.mmesh.message.MessageQueue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.Destination;
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
                this.destinations.get(message.getDestination()).put(message);
                break;
                
            default:
                throw new UnsupportedOperationException("Should not be routing: " + message.getType().name());
        }
    }
    
    public final void setDestinations(Collection<Destination> destinations) {
        Preconditions.checkArgument(destinations != null && !destinations.isEmpty());
        
        this.destinations.clear();
        
        for (Destination dest : destinations) {
            this.destinations.put(dest.getIdentifier(), dest.getQueue());
        }
    }
}

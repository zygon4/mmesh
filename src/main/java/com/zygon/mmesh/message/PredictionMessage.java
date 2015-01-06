
package com.zygon.mmesh.message;

import com.zygon.mmesh.Identifier;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 *
 * @author zygon
 */
public class PredictionMessage extends Message {
    
    private final Identifier source;
    private final String display;
    private final Set<Identifier> actives;
    
    public PredictionMessage(Identifier source, Identifier destination, double value, long timestamp, Set<Identifier> actives) {
        super(Type.PREDICTION, destination, value, timestamp);
        this.source = source;
        this.actives = Collections.unmodifiableSet(actives);
        
        StringBuilder activeSB = new StringBuilder();
        
        for (Identifier id : this.actives) {
            activeSB.append(id);
            activeSB.append(",");
        }
        
        this.display = this.getType().name() + "," + "[source:" + this.source + ", dest:" + 
                this.getDestination() + ", actives: " + activeSB.toString() + "]," + this.getValue() + "," + new Date(this.getTimestamp());
    }
    
    public PredictionMessage(Identifier source, Identifier destination, double value, Set<Identifier> actives) {
        this(source, destination, value, System.currentTimeMillis(), actives);
    }

    public Set<Identifier> getActives() {
        return this.actives;
    }

    public Identifier getSource() {
        return this.source;
    }

    @Override
    public String getDisplay() {
        return this.display;
    }
    
    public Message setSource(Identifier source) {
	return new PredictionMessage(source, this.getDestination(), this.getValue(), this.getTimestamp(), this.getActives());
    }
}

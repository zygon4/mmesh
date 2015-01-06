
package com.zygon.mmesh.message;

import com.zygon.mmesh.Identifier;
import java.util.Date;

/**
 *
 * @author zygon
 */
public class Message {
    
    public static enum Type {
        ACTIVATION,
        PREDICTION,
    }
    
    private final Type type;
    private final Identifier destination;
    private final double value;
    private final long timestamp;
    
    private final String display;

    public Message(Type type, Identifier destination, double value, long timestamp) {
        this.type = type;
        this.value = value;
        this.destination = destination;
        this.timestamp = timestamp;
        
        this.display = this.type.name() + "," + "[dest:" + this.destination + "]," + this.value + "," + new Date(this.timestamp);
    }
    
    public Identifier getDestination() {
        return destination;
    }

    public String getDisplay() {
        return display;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public Type getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public Message setDestination(Identifier dest) {
	return new Message(this.type, dest, this.value, this.timestamp);
    }

    @Override
    public String toString() {
        return this.display;
    }
}

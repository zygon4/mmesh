
package com.zygon.mmesh.message;

import com.zygon.mmesh.Identifier;

/**
 *
 * @author zygon
 */
public class ActivationMessage extends Message {
    
    public ActivationMessage(Identifier destination, double value, long timestamp) {
        super(Message.Type.ACTIVATION, destination, value, timestamp);
    }
    
    public ActivationMessage(Identifier destination, double value) {
        this(destination, value, System.currentTimeMillis());
    }
}

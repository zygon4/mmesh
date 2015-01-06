
package com.zygon.mmesh.sdr;

import com.google.common.base.Preconditions;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.core.CellGroup;
import com.zygon.mmesh.message.ActivationMessage;

/**
 * Sparse Distributed Representations are created from sensory input.  They 
 * represent a competitive vector of the total input at time T.
 *
 * @author zygon
 */
public class SDR {
    
    private CellGroup cellGroup;

    public SDR (CellGroup cellGroup) {
        Preconditions.checkArgument(cellGroup != null);
        
        this.cellGroup = cellGroup;
    }
    
    // The usage of this is not quite clear, sending sources could be 
    // a silly idea. If we don't send sources, then we can probably just
    // put the targets in the constructor as original intended.
    public void activate(Identifier[] sourceIds, Identifier[] targetIds) {
        Preconditions.checkArgument(sourceIds != null && sourceIds.length != 0);
        Preconditions.checkArgument(targetIds != null && targetIds.length != 0);
        Preconditions.checkArgument(sourceIds.length == targetIds.length);
        
        // Could check for sparseness value to be within range. ie just sparse
        // enough and just distributed enough
        
        this.cellGroup.reset();
        
        for (int i = 0; i < sourceIds.length; i++) {
            ActivationMessage msg = new ActivationMessage(targetIds[i], (i+1) * 10, System.currentTimeMillis());
            
            this.cellGroup.send(msg);
        }
        
//        while (this.cellGroup.getActiveCells().length != sourceIds.length) {
            // Block and wait for activations
            // TBD: infinite timeout possible
//            try { Thread.sleep(500); } catch (Throwable ignore) {}
//        }
    }
    
    public Identifier[] getActive() {
        return this.cellGroup.getActiveCells();
    }
}

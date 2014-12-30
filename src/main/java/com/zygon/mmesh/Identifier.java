
package com.zygon.mmesh;

import com.google.common.base.Preconditions;
import java.util.Arrays;

/**
 * This impl might change tremendously
 *
 * @author zygon
 */
public class Identifier implements Comparable<Identifier> {

    private final int[] coordinates;
    private final int dims;
    private final String display;
    
    private int hash = -1;

    public Identifier(int ...coords) {
        Preconditions.checkArgument(coords != null);
        Preconditions.checkArgument(coords.length > 0);
        
        this.coordinates = coords;
        this.dims = this.coordinates.length;
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < this.coordinates.length; i++) {
            sb.append(this.coordinates[i]);
            if (i < this.coordinates.length - 1) {
                sb.append("_");
            }
        }
        
        this.display = sb.toString();
    }
    
    /*
    (defn- dis [x y]
        (math/sqrt (reduce + (map (fn [a b] (math/expt (- a b) 2)) x y))))
    */
    public double getDistance(Identifier o) {
        
        Preconditions.checkArgument(o.dims == this.dims);
        
        double total = 0.0;
        
        for (int i = 0; i < this.coordinates.length; i++) {
            total += Math.pow(this.coordinates[i] - o.coordinates[i], 2);
        }
        
        return Math.sqrt(total);
    }
    
    @Override
    public int compareTo(Identifier o) {
        return o.hashCode() > this.hashCode() ? -1 : (o.hashCode() < this.hashCode() ? 1 : 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (!(obj instanceof Identifier)) {
            return false;
        }
        
        return this.hashCode() == ((Identifier)obj).hashCode();
    }

    @Override
    public final int hashCode() {
        
        if (this.hash == -1) {
            this.hash = Arrays.hashCode(this.coordinates);
        }
        
        return this.hash;
    }
    
    public String getDisplay() {
        return this.display;
    }

    @Override
    public String toString() {
        return this.getDisplay();
    }
}

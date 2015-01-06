
package com.zygon.mmesh.core;

import com.google.common.collect.Sets;
import com.zygon.mmesh.Identifier;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author zygon
 */
public class IdentifierSet extends AbstractSet implements Comparable<IdentifierSet> {

    private final Set<Identifier> identifiers;
    private int hash = -1;

    public IdentifierSet(Set<Identifier> identifiers) {
        this.identifiers = Collections.unmodifiableSet(Sets.newTreeSet(identifiers));
    }

    @Override
    public int compareTo(IdentifierSet t) {
        return this.hashCode() > t.hashCode() ? 1 : (this.hashCode() < t.hashCode() ? -1 : 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof IdentifierSet)) {
            return false;
        }
        
        return this.hashCode() == o.hashCode();
    }

    public Identifier[] getIdentifiers() {
        return this.identifiers.toArray(new Identifier[this.identifiers.size()]);
    }
    
    @Override
    public int hashCode() {
        if (hash == -1) {
            this.hash = Arrays.hashCode(this.identifiers.toArray(new Identifier[this.identifiers.size()]));
        }
        
        return this.hash;
    }

    @Override
    public Iterator iterator() {
        return this.identifiers.iterator();
    }

    @Override
    public int size() {
        return this.identifiers.size();
    }
}

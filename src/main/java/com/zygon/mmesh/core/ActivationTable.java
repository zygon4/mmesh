
package com.zygon.mmesh.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.ActivationMessage;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zygon
 */
public class ActivationTable {

    private static LoadingCache<Identifier, Identifier> createIdentifierCache (long timeout, TimeUnit units) {
        
        LoadingCache<Identifier, Identifier> cache = 
            CacheBuilder.newBuilder()
                .expireAfterWrite(timeout, units)
                .build(
                    new CacheLoader<Identifier, Identifier>() {
                        @Override
                        public Identifier load(Identifier key) throws Exception {
                            return key;
                        }
                    }
                );
        
        return cache;
    }
    
    private final LoadingCache<Identifier,Identifier> cache = createIdentifierCache(1, TimeUnit.SECONDS);

    public final void add (ActivationMessage msg) throws ExecutionException {
        this.cache.put(msg.getDestination(), msg.getDestination());
    }
    
    public final long getCount() {
        return this.cache.size();
    }
    
    public final Set<Identifier> getAllIdentifiers() {
        
        Set<Identifier> sources = Sets.newHashSet();
        
        // light copy - hopefully this won't cause issues
        for (Identifier id : this.cache.asMap().values()) {
            sources.add(id);
        }
        
        return sources;
    }
    
    public final boolean contains(Identifier id) {
        return this.cache.getIfPresent(id) != null;
    }
    
    public final boolean isEmpty() {
        return this.cache.size() == 0;
    }
}

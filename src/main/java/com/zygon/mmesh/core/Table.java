
package com.zygon.mmesh.core;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.Message;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * The purpose of the tables is to help prediction. One table instance will
 * hold recent activations, and one will hold expected predictions.
 *
 * @author zygon
 */
public class Table {
    
    /**
     * Returns a time-expiring cache of messages by (source) Identifier.
     * @param timeout
     * @param units
     * @return 
     */
    private static LoadingCache<Identifier, Collection<Message>> createCache (long timeout, TimeUnit units) {
        
        LoadingCache<Identifier, Collection<Message>> cache = 
            CacheBuilder.newBuilder()
                .expireAfterWrite(timeout, units)
                .build(
                    new CacheLoader<Identifier, Collection<Message>>() {
                        @Override
                        public Collection<Message> load(Identifier key) throws Exception {
                            return Lists.newArrayList();
                        }
                    }
                );
        
        return cache;
    }
    
    private static long fib(long i) {
        if (i < 2) {
            return i;
        } else {
            return fib(i - 1) + fib(i - 2);
        }
    }
    
    private static long getCacheTimeout (long idxValue) {
        // seed fibonaci at 5 to get some larger values
        return (fib (5 + idxValue));
    }
    
    private static long getCacheIndex(double messageValue) {
        return Math.round(Math.log(messageValue));
    }
    
    private static final boolean VERBOSE = false;
    
    private final Multimap<Identifier, Message> messages = Multimaps.newListMultimap(
            new TreeMap<Identifier, Collection<Message>>(), 
            new Supplier<List<Message>>() {
                @Override
                public List<Message> get() {
                    return Lists.newArrayList();
                }
            }
        );
    
    // TBD: name
    private final Map<Long, LoadingCache<Identifier, Collection<Message>>> cachesByArbitraryValue = Maps.newHashMap();
    
    private final String name;
    
    public Table(String name) {
        this.name = name;
    }
    
    private static final long INPUT_TIME_ALLOWANCE = 100; // ms - arbitrary
    
    public boolean add (final Message msg) throws ExecutionException {
        
        // Here we are filtering out messages that came in at the same time.
        // This is really meant to curb the issue of overzealous propagation,
        // this can probably be designed differently.
        
        // Probably just need to use filterEntries but whatever
        Multimap<Identifier, Message> filterEntries
            = Multimaps.filterEntries(this.messages, new Predicate<Map.Entry<Identifier, Message>>() {
                @Override
                public boolean apply(Map.Entry<Identifier, Message> input) {
                    // This means that someone came in too recently with a message from some source.
                    return Math.abs(msg.getTimestamp() - input.getValue().getTimestamp()) < INPUT_TIME_ALLOWANCE;
                }
            });
        
        boolean putMessage = false;
        
        if (filterEntries.isEmpty()) {
            // storing messages by source
            this.messages.put(msg.getSource(), msg);
            putMessage = true;
            
            /////////////////////////////////////////////
            // Still utilizing the filter code for now
            
            long idx = getCacheIndex(msg.getValue());
        
            LoadingCache<Identifier, Collection<Message>> cache = this.cachesByArbitraryValue.get(idx);

            if (cache == null) {
                long cacheTimeout = getCacheTimeout(idx);
                if (VERBOSE) {
                    System.out.println("New cache timeout: " + cacheTimeout);
                }
                cache = createCache(cacheTimeout, TimeUnit.SECONDS);
                this.cachesByArbitraryValue.put(idx, cache);
            }
            
            Collection<Message> messagesForSource = cache.get(msg.getSource());
            messagesForSource.add(msg);
            
        } else {
            // just dump some stdout for now
            if (VERBOSE) {
                System.out.println("filtering out msg");
            }
        }
        
        return putMessage;
    }
    
    public Map<Identifier, Double> getTotalValuesByIdentifiers() {
        Map<Identifier, Double> totalValuesByIdentifiers = Maps.newHashMap();
        
        for (LoadingCache<Identifier, Collection<Message>> cache : this.cachesByArbitraryValue.values()) {
            
            ConcurrentMap<Identifier, Collection<Message>> messagesById = cache.asMap();
            
            for (Map.Entry<Identifier, Collection<Message>> messages : messagesById.entrySet()) {
                
                Identifier key = messages.getKey();
                Collection<Message> messageValues = messages.getValue();
                
                double total = 0.0;
                
                for (Message msg : messageValues) {
                    total += msg.getValue();
                }
                
                Double totalValue = totalValuesByIdentifiers.get(key);
                
                if (totalValue == null) {
                    totalValue = total;
                } else {
                    totalValue += total;
                }
                
                totalValuesByIdentifiers.put(key, totalValue);
            }
        }
        
        return totalValuesByIdentifiers;
    }
    
    public Collection<Identifier> getAllIdentifiers() {
        
        Collection<Identifier> sources = Sets.newHashSet();
        
        // light copy - hopefully this won't cause issues
        for (LoadingCache<Identifier, Collection<Message>> cache : this.cachesByArbitraryValue.values()) {
            sources.addAll(cache.asMap().keySet());
        }
        
        return sources;
    }
    
    public boolean hasReferences(Identifier id) {
        
        for (LoadingCache<Identifier, Collection<Message>> cache : this.cachesByArbitraryValue.values()) {
            Collection<Message> messagesIfLoaded = cache.getIfPresent(id);
            if (messagesIfLoaded != null) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasReferences() {
        
        for (LoadingCache<Identifier, Collection<Message>> cache : this.cachesByArbitraryValue.values()) {
            if (cache.size() > 0) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public String toString() {
        
        Map<Identifier, Double> totalValuesByIdentifiers = this.getTotalValuesByIdentifiers();
        
        double totalActivation = 0.0;
        
        for (Double val : totalValuesByIdentifiers.values()) {
            totalActivation += val;
        }
        
        return "[" + this.name + ": " + totalActivation + "]";
    }
}

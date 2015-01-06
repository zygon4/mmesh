
package com.zygon.mmesh.core;

import com.google.common.collect.Maps;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.PredictionMessage;
import java.util.Map;
import java.util.TreeMap;

/**
 * This holds potential predictions. These messages hold the presently active 
 * cells when the predictions came to be. We need to aggregate the messages
 * when multiple sets overlap in identifiers, and split when the messages can
 * make separate groups.
 *
 * @author zygon
 */
public class PredictionTable {
    
    public static final double NULL_VALUE = -1.0;
    
    private static long fib(long i) {
        if (i < 2) {
            return i;
        } else {
            return fib(i - 1) + fib(i - 2);
        }
    }
    
    public static int longestSubstr(Identifier[] first, Identifier[] second) {

        if (first == null || second == null || first.length == 0 || second.length == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length;
        int sl = second.length;
        int[][] table = new int[fl + 1][sl + 1];

        for (int s = 0; s <= sl; s++) {
            table[0][s] = 0;
        }
        for (int f = 0; f <= fl; f++) {
            table[f][0] = 0;
        }

        for (int i = 1; i <= fl; i++) {
            for (int j = 1; j <= sl; j++) {
                if (first[i - 1].equals(second[j - 1])) {
                    if (i == 1 || j == 1) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
            }
        }
        return maxLen;
    }
    
    // use concurrent map?
    private final TreeMap<IdentifierSet,Double> predictionValuesByIdSet = Maps.newTreeMap();
    
    public synchronized void add (IdentifierSet idSet, double value) {
        
        Double exactMatchValue = this.predictionValuesByIdSet.get(idSet);
        
        if (exactMatchValue != null) {
            // exact match! This is good as it exactly re-enforces another 
            // prediction of ours.
            
            double incomingValue = fib(idSet.size()) * value;
            
            this.predictionValuesByIdSet.put(idSet, exactMatchValue + incomingValue);
        } else {
            // We didn't find an exact copy - try and find the longest substrings
            Identifier[] ids = idSet.getIdentifiers();
            Map<IdentifierSet,Integer> longestMatchingSubstrings = Maps.newHashMap();
            
            int longest = 0;
            
            for (IdentifierSet key : this.predictionValuesByIdSet.keySet()) {
                int subStringLength = longestSubstr(key.getIdentifiers(), ids);
                if (subStringLength > longest) {
                    longestMatchingSubstrings.clear();
                    longestMatchingSubstrings.put(key, subStringLength);
                } else if (subStringLength == longest) {
                    longestMatchingSubstrings.put(key, subStringLength);
                }
            }
            
            if (!longestMatchingSubstrings.isEmpty()) {
                // the list of IdentifierSets are the best matches, they should 
                // be rewarded.
                for (Map.Entry<IdentifierSet,Integer> rewardEntry : longestMatchingSubstrings.entrySet()) {
                    double originalFullValue = this.predictionValuesByIdSet.get(rewardEntry.getKey());
                    int substringSize = rewardEntry.getValue();
                    double incomingValue = fib(substringSize) * value;

                    this.predictionValuesByIdSet.put(rewardEntry.getKey(), originalFullValue + incomingValue);
                }
            }
            
            // Add the new prediction set - it hasn't been seen before.
            double incomingValue = fib(idSet.size()) * value;
            this.predictionValuesByIdSet.put(idSet, incomingValue);
        }
    }
    
    /*pkg*/ void add(PredictionMessage msg) {
        IdentifierSet idSet = new IdentifierSet(msg.getActives());
        this.add(idSet, msg.getValue());
    }
    
    public final long getCount() {
        return this.predictionValuesByIdSet.size();
    }
    
    public synchronized String getDisplay() {
        StringBuilder sb = new StringBuilder();
        
        int i = 0;
        
        for (Map.Entry<IdentifierSet,Double> entry : this.predictionValuesByIdSet.entrySet()) {
            sb.append(entry.getKey());
            sb.append(" - ");
            sb.append(entry.getValue());

            if (i < this.predictionValuesByIdSet.size() - 1) {
                sb.append("\n");
            }

            i++;
        }
        
        return sb.toString();
    }
    
    // TBD: multiple predictions based on similarity?
    public double getPrediction(IdentifierSet identifierSet) {
        Double predictionValue = this.predictionValuesByIdSet.get(identifierSet);
        return predictionValue != null ? predictionValue : NULL_VALUE;
    }

    public final boolean isEmpty() {
        return this.predictionValuesByIdSet.isEmpty();
    }
    
    @Override
    public String toString() {
        return this.getDisplay();
    }
}

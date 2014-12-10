
package com.zygon.mmesh.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.Message;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author zygon
 */
public class CellGroup {
    
    private static final class Watcher extends Thread {

        private final Cell[] cells;
        
        public Watcher(Cell[] cells) {
            super("Watcher");
            super.setDaemon(true);
            
            this.cells = cells;
        }

        @Override
        public void run() {
            while (true) {
                
                try { Thread.sleep(2000); } catch (Throwable ignore) {}
                
                StringBuilder sb = new StringBuilder();
                
                for (Cell cell : this.cells) {
                    sb.append(cell);
                    sb.append(" ");
                }
                
                System.out.println(sb);
            }
        }
    }
    
    // 1D dimensional for now
    private static Collection<Cell> getNeighbors(int idx, Cell[] cells, int radius) {
        Collection<Cell> neighbors = Lists.newArrayList();
        
        int min = Math.max(idx - (radius / 2), 0);
        int max = Math.min(idx + (radius / 2), cells.length);
        
        for (int i = min; i < max; i++) {
            if (i != idx) {
                neighbors.add(cells[i]);
            }
        }
        
        return neighbors;
    }
    
    private static final AbstractScheduledService.Scheduler CELL_SCHEDULER = 
            AbstractScheduledService.Scheduler.newFixedRateSchedule(0, 1, TimeUnit.SECONDS);
    
    private final Identifier groupId;
    private final Map<Identifier,Cell> cellsById = Maps.newHashMap();
    
    // Consider an Identifer generator for different cell configurations
    // Also, maybe just plain old cell configuration properties like radius
    public CellGroup(Identifier groupId, int cellCount) {
        Preconditions.checkArgument(groupId != null);
        Preconditions.checkArgument(cellCount > 0);
        
        this.groupId = groupId;
        
        Cell[] cells = new Cell[cellCount];
        
        // Create cells - just 1D for now
        for (int i = 0; i < cellCount; i++) {
            cells[i] = new Cell(new Identifier(i), CELL_SCHEDULER);
        }
        
        // Attach neighbors
        for (int i = 0; i < cellCount; i++) {
            Collection<Cell> neighbors = getNeighbors(i, cells, 4);
            cells[i].setNeighbors(neighbors);
        }
        
        // put into map
        for (int i = 0; i < cellCount; i++) {
            this.cellsById.put(cells[i].getIdentifier(), cells[i]);
        }
    }
    
    public void doStart() {
        // Start cells
        for (Cell cell : this.cellsById.values()) {
            cell.startAsync();
        }
        
//      Start simple watcher
//        new Watcher(cells).start();
    }
    
    public void doStop() {
        // Stop cells
        for (Cell cell : this.cellsById.values()) {
            cell.stopAsync();
        }
    }
    
    public void send(Message message) {
        Preconditions.checkArgument(this.cellsById.containsKey(message.getDestination()), "Destination not found for: " + message.getDestination());
        
        this.cellsById.get(message.getDestination()).getInputQueue().put(message);
    }
    
    public void send(Message ...messages) {
        for (Message msg : messages) {
            this.send(msg);
        }
    }
}

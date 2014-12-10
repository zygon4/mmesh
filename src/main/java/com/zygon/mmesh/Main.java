package com.zygon.mmesh;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import com.zygon.mmesh.core.Cell;
import com.zygon.mmesh.message.Message;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;


public class Main {
    
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
    
    private static final int CELL_COUNT = 10;
    
    public static void main(String[] args) throws IOException {
        
        Scheduler scheduler = Scheduler.newFixedRateSchedule(0, 5, TimeUnit.SECONDS);
        
        Cell[] cells = new Cell[CELL_COUNT];
        
        // Create cells
        for (int i = 0; i < CELL_COUNT; i++) {
            cells[i] = new Cell(new Identifier(i), scheduler);
        }
        
        // Attach neighbors
        for (int i = 0; i < CELL_COUNT; i++) {
            Collection<Cell> neighbors = getNeighbors(i, cells, 4);
            cells[i].setNeighbors(neighbors);
        }
        
        // Start cells
        for (Cell cell : cells) {
            cell.startAsync();
        }
        
        // Start simple watcher
//        new Watcher(cells).start();
        
        // Send an activation
        
        for (int i = 1; i <= 10; i++) {
            
            Identifier source1 = new Identifier(0);
            Identifier target1 = new Identifier(4);
            cells[0].getInputQueue().put(new Message(Message.Type.ACTIVATION, source1, target1, i * 10, System.currentTimeMillis()));

            try { Thread.sleep(2000); } catch (Throwable ignore) {}
            
            Identifier source2 = new Identifier(4);
            Identifier target2 = new Identifier(9);
            cells[4].getInputQueue().put(new Message(Message.Type.ACTIVATION, source2, target2, i * 10, System.currentTimeMillis()));
            
            try { Thread.sleep(2000); } catch (Throwable ignore) {}
            
//            Identifier source3 = new Identifier(CELL_COUNT - 1);
//            Identifier target3 = new Identifier(0);
//            cells[CELL_COUNT - 1].getInputQueue().put(new Message(Message.Type.ACTIVATION, source3, target3, 100.0, System.currentTimeMillis()));
            
            
        }
        
//        System.out.println("Enter any key to continue...");
//        System.in.read();
        
        // Stop cells
        for (Cell cell : cells) {
            cell.stopAsync();
        }
    }
}

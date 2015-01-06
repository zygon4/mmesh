package com.zygon.mmesh;

import com.zygon.mmesh.core.CellGroup;
import com.zygon.mmesh.sdr.SDR;
import java.io.IOException;


public class Main {
    
    private static final int CELL_COUNT = 10;
    
    public static void main(String[] args) throws IOException {
        
        CellGroup cellGroup = new CellGroup(new Identifier(0), CELL_COUNT);
        cellGroup.doStart();
        
        SDR sdr = new SDR(cellGroup);
        
        for (int i = 1; i <= 3; i++) {
            
            int prevSourceId = -1;
            int sourceId = -1;
            int destId = -1;
            
            for (int j = 0; j < 10; j++) {
                
                if (prevSourceId == -1) {
                    sourceId = 0;
                    prevSourceId = 0;
                } else {
                    prevSourceId = destId;
                    sourceId = prevSourceId;
                }
                
                destId = j;
                
                Identifier source = new Identifier(sourceId);
                Identifier target = new Identifier(destId);
                
                sdr.activate(new Identifier[]{source}, new Identifier[]{target});
                
                try { Thread.sleep(200); } catch (Throwable ignore) {}
            }
        }
        
//        System.out.println("Enter any key to continue...");
//        System.in.read();

        cellGroup.doStop();
    }
}

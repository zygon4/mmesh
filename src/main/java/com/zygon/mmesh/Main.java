package com.zygon.mmesh;

import com.zygon.mmesh.core.CellGroup;
import com.zygon.mmesh.message.Message;
import java.io.IOException;


public class Main {
    
    private static final int CELL_COUNT = 10;
    
    public static void main(String[] args) throws IOException {
        
        CellGroup cellGroup = new CellGroup(new Identifier(0), CELL_COUNT);
        
        cellGroup.doStart();
        
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
                
                cellGroup.send(new Message(Message.Type.ACTIVATION, source, target, i * 10, System.currentTimeMillis()));
                
                try { Thread.sleep(1000); } catch (Throwable ignore) {}
            }
            
            try { Thread.sleep(1000); } catch (Throwable ignore) {}
        }
        
//        System.out.println("Enter any key to continue...");
//        System.in.read();

        cellGroup.doStop();
    }
}

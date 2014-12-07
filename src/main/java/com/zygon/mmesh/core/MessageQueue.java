
package com.zygon.mmesh.core;

import com.google.common.collect.Queues;
import com.zygon.mmesh.message.Message;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author zygon
 */
public class MessageQueue {
    private final ArrayBlockingQueue<Message> queue = Queues.newArrayBlockingQueue(1024);
    
    public void put (Message msg) {
        try {
            this.queue.put(msg);
        } catch (InterruptedException e) {
            // just dump for now
//            e.printStackTrace();
        }
    }
    
    public Message get() {
        try {
            return this.queue.take();
        } catch (InterruptedException e) {
            // just dump for now
//            e.printStackTrace();
        }
        
        return null;
    }
}

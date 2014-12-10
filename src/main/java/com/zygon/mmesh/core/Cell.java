
package com.zygon.mmesh.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.Message;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 
 * @author zygon
 */
public class Cell extends AbstractScheduledService {
    
    public static class CellPrinter {
    
        private final Cell cell;

        public CellPrinter(Cell cell) {
            this.cell = cell;
        }
        
        private String format(Table table) {
            StringBuilder sb = new StringBuilder();

            double total = 0.0;
            for (Map.Entry<Identifier, Double> totalBySourceId : table.getTotalValuesByIdentifiers().entrySet()) {
                total += totalBySourceId.getValue();
            }
            total = Math.min(total, 100.0);

            sb.append(String.format("%5.2f", total));
            
            return sb.toString();
        }
        
        public String print() {
            return "["+this.cell.id + ":" + format(this.cell.activationTable) + " | " + format(this.cell.predictionTable) +"]";
        }
    }
    
    private static final boolean VERBOSE = false;
    
    private final Table activationTable = new Table("Activations");
    private final Table predictionTable = new Table("Predictions");
    private final MessageQueue inputQueue = new MessageQueue();

    private final Identifier id;
    private final Scheduler scheduler;
    private final Router router;
    
    public Cell(Identifier id, Scheduler scheduler) {
        super();
        Preconditions.checkArgument(id != null);
        Preconditions.checkArgument(scheduler != null);
        
        this.id = id;
        this.scheduler = scheduler;
        this.router = new Router(this.id);
    }

    // TBD: drop off should be proportional to the number of cells. Ideally, all
    // cells should receive some form of activation.
    private static final double RESIDUAL_DROP_OFF = .5;
    private static final double RESIDUAL_CUTOFF = 10;

    public CellPrinter getPrinter() {
        return new CellPrinter(this); // just a new one for now
    }
    
    public Identifier getIdentifier() {
        return this.id;
    }

    // This should probably be package scoped. There should be an input controller
    // do-hickey in the core package.
    public MessageQueue getInputQueue() {
        return this.inputQueue;
    }
    
    private boolean requiresActivationPropagation(Message incomingActivation) {
        return incomingActivation.getValue() >= RESIDUAL_CUTOFF;
    }
    
    private void propagateResidualActivation(Message msg) {
        if (this.requiresActivationPropagation(msg)) {
            double outgoingMessageValue = msg.getValue() * RESIDUAL_DROP_OFF;
            Message outgoingMessages = new Message(Message.Type.RESIDUAL, this.id, null, outgoingMessageValue, new Date().getTime());
            this.router.send(msg.getSource(), outgoingMessages);
        }
    }
    
    @Override
    protected void runOneIteration() throws Exception {
        while (this.inputQueue.hasMessage()) {
            try {
                Message incomingMessage = this.inputQueue.get();

                if (incomingMessage != null) {
                    switch (incomingMessage.getType()) {
                        case ACTIVATION:
                            boolean addedMessage = this.activationTable.add(incomingMessage);

                            if (addedMessage) {
                                if (VERBOSE) {
                                    System.out.println(this.id + " received message: " + incomingMessage);
                                }
                                
                                // check if it was predicted or not, if no one saw this
                                // coming, then it was a anomaly and we should attempt
                                // to populate a prediction based on who was just active
                                if (!this.predictionTable.hasReferences()) {
                                    if (VERBOSE) {
                                        System.out.println(this.id + ": ANOMALY DETECTION FROM " + incomingMessage.getSource());
                                    }

                                    for (Map.Entry<Identifier, Double> totalById : this.activationTable.getTotalValuesByIdentifiers().entrySet()) {
                                        double value = Math.min(totalById.getValue(), 100.0);
                                        
                                        // Using the previously-active cell as the source. We're basically
                                        // just injecting a prediction for them to get started.
                                        this.predictionTable.add(new Message(Message.Type.PREDICTION, totalById.getKey(), this.id, value, System.currentTimeMillis()));
                                    }
                                }
                                
                                // Propagate activation
                                this.propagateResidualActivation(incomingMessage);

                                // Send prediction feedback
                                this.sendPredictionFeedback();
                            }
                            break;

                        case PREDICTION:

                            // Someone thinks we predict them 
                            if (incomingMessage.getDestination().equals(this.id)) {
                                if (VERBOSE) {
                                    System.out.println(this.id + " received message: " + incomingMessage);
                                }
                                this.predictionTable.add(incomingMessage);
                            } else {
                                if (VERBOSE) {
                                    System.out.println(this.id + " forwarding message: " + incomingMessage);
                                }
                                // route someone else's prediction message
                                this.router.send(incomingMessage.getSource(), incomingMessage);
                            }
                            break;
                            
                        case RESIDUAL:
                            boolean addedResidualMsg = this.activationTable.add(incomingMessage);

                            if (addedResidualMsg) {
                                if (VERBOSE) {
                                    System.out.println(this.id + " received message: " + incomingMessage);
                                }
                                
                                // Propagate activation
                                this.propagateResidualActivation(incomingMessage);

                                // Send prediction feedback
                                this.sendPredictionFeedback();
                            }
                            break;
                    }
                } else {
                    if (this.isRunning()) {
                        System.out.println("BAD - should have blocked on receive");
                    }
                }
            } catch (ExecutionException ee) {
                if (this.isRunning()) {
                    ee.printStackTrace();
                }
            }
        }
    }
    
    @Override
    protected Scheduler scheduler() {
        return this.scheduler;
    }
    
    private void sendPredictionFeedback() {
        for (Map.Entry<Identifier, Double> totalBySourceId : this.predictionTable.getTotalValuesByIdentifiers().entrySet()) {
            double value = Math.min(totalBySourceId.getValue(), 100.0);

            value = Math.log(value);
            // This is up in the air: we're using the total activation value
            // from one particular source.  If they did predict us.

            if (value > 0) {
                System.out.println(totalBySourceId.getKey() + " => " + this.id + " | " + value);
                Message outgoingMessages = new Message(Message.Type.PREDICTION, this.id, totalBySourceId.getKey(), value, new Date().getTime());
                this.router.send(this.id, outgoingMessages);
            }
        }
    }

    public void setNeighbors(Collection<Cell> neighbors) {
        this.router.setDestinations(neighbors);
    }
    
    @Override
    public String toString() {
        return "{" + this.id + ": A_" + this.activationTable + "|P_" + this.predictionTable + "}";
    }
}

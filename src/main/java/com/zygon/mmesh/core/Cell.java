
package com.zygon.mmesh.core;

import com.zygon.mmesh.message.Router;
import com.zygon.mmesh.message.MessageQueue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zygon.mmesh.Identifier;
import com.zygon.mmesh.message.ActivationMessage;
import com.zygon.mmesh.message.Destination;
import com.zygon.mmesh.message.Message;
import com.zygon.mmesh.message.PredictionMessage;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * TBD: consider a watcher thread to check on active duty cycles
 *      and "boost" mechanics.
 *
 * @author zygon
 */
public class Cell extends AbstractScheduledService implements Destination {
    
    public static class CellPrinter {
    
        private final Cell cell;

        public CellPrinter(Cell cell) {
            this.cell = cell;
        }
        
        private String format(ActivationTable table) {
            StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            for (Identifier totalBySourceId : table.getAllIdentifiers()) {
                sb.append(totalBySourceId.getDisplay());
                sb.append(" ");
            }
            sb.append("}");
            
            return sb.toString();
        }
        
        public String print() {
            return "[Id: "+this.cell.id + "\nA:\n" + format(this.cell.activeTable) + " |\nP:\n" + this.cell.predictionTable +"]";
        }
    }
    
    private static final boolean VERBOSE = true;
    
    // This table holds the present activations
    private final ActivationTable activeTable = new ActivationTable();
    
    // Thie table holds the present predictions
    private final PredictionTable predictionTable = new PredictionTable();
    
    private final MessageQueue inputQueue = new MessageQueue();

    private final CellGroup cellGroupId;
    private final Identifier id;
    private final Scheduler scheduler;
    private final Router router;
    
    private volatile boolean isActive = false;
    private volatile boolean isPredicted = false;
    
    public Cell(CellGroup cellGroupId, Identifier id, Scheduler scheduler) {
        super();
        Preconditions.checkNotNull(cellGroupId);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(scheduler);
        
        this.cellGroupId = cellGroupId;
        this.id = id;
        this.scheduler = scheduler;
        this.router = new Router(this.id);
    }

    /**
     * How many predictors must be active to send positive feedback.  This 
     * may want to become dynamic depending on the numbers of cells, etc.
     */
    private static final int PREDICTION_DUTY_MIN = 1;
    
//    private Map<Identifier,Double> getPredictors() {
//        Map<Identifier,Double> predictionsById = Maps.newHashMap(this.predictionTable.getTotalValuesByIdentifiers());
//
//        Iterator<Identifier> iter = predictionsById.keySet().iterator();
//        while (iter.hasNext()) {
//            Identifier id = iter.next();
//
//            double value = Math.min(predictionsById.get(id), 100.0);
//
//            // TBD: prediction threshold
//            if (value <= 0) {
//                iter.remove();
//            }
//        }
//
//        return predictionsById;
//    }
    
    public CellPrinter getPrinter() {
        return new CellPrinter(this); // just a new one for now
    }

    public final CellGroup getCellGroupId() {
        return this.cellGroupId;
    }
    
    public final Identifier getIdentifier() {
        return this.id;
    }

    // This should probably be package scoped. There should be an input controller
    // do-hickey in the core package.
    public final MessageQueue getQueue() {
        return this.inputQueue;
    }
    
    public final Router getRouter() {
        return this.router;
    }
    
    // TODO: this might go away
    /*pkg*/ void reset() {
        // learning step  
        //      - if we were active and predicted then send positive
        //        prediction feedback
        //      - if were not active but predicted, then send negative 
        //        prediction feedback
         
        
        // TBD: could be async
//        this.sendPredictionFeedback();
        
        this.isActive = false;
    }
    
    protected void handleOtherActivation(ActivationMessage message) throws ExecutionException {
        Preconditions.checkArgument(!message.getDestination().equals(this.id));
        
        this.activeTable.add(message);
        
        this.isPredicted = this.predictionTable.getPrediction(new IdentifierSet(this.activeTable.getAllIdentifiers())) != PredictionTable.NULL_VALUE;
    }
    
    private void sendPredictions(Set<Identifier> activeIdentifiers) {
        for (Identifier dest : activeIdentifiers) {
            double value = activeIdentifiers.size();

            // Quickly construct a new active set without the destination
            // We don't need to tell the co-active cells that they are also
            // active - it's implied.
            // E.g. If I'm '3', we can send "1,2" to '1' and '2' but they
            // don't respectively need to know that they are active.
            Set<Identifier> activeIds = Sets.newHashSet();

            for (Identifier activeId : activeIdentifiers) {
                if (!activeId.equals(dest)) {
                    activeIds.add(activeId);
                }
            }

            if (!activeIds.isEmpty()) {
                PredictionMessage prediction = new PredictionMessage(this.id, dest, value, activeIds);
                Preconditions.checkState(!prediction.getActives().contains(dest));

                this.router.send(this.id, prediction);
            }
        }
    }
    
    protected void handleSelfActivation(ActivationMessage message) throws ExecutionException {
        Preconditions.checkArgument(message.getType() == Message.Type.ACTIVATION);
        Preconditions.checkArgument(message.getDestination().equals(this.id));
        
        this.cellGroupId.notifyActive(this.id);
        this.isActive = true;
        
        Set<Identifier> activeIdentifiers = this.activeTable.getAllIdentifiers();
        
        // Yay! we were predicted
        if (this.isPredicted) {
            try {
                this.sendPredictionFeedback();
            } finally {
                this.isPredicted = false;
            }
        } else {
            
            if (VERBOSE) {
                System.out.println(this.id + ": ANOMALY DETECTION");
            }
            
            // We weren't predicted - but maybe we can make a suggestion -
            // if we had active IDs immediately before this cell became active
            // then, we can suggest a prediction.
            
            if (!activeIdentifiers.isEmpty()) {
                
                // TBD: how to avoid calling something a prediction
                // when infact it's just part of a static image (for instance)?
                // When does that temporal scene-slicing happen?
                // If we learn the static "images" then the behavior
                // is really just spatial pooling? maybe it's a blend
                // What happens if we learn our co-active cells at this
                // timestep plus previously active cells - is that terrible?
                // Send a prediction to each of the cells that were just active
                // Everyone will know about everyone who is also active right now.

                this.sendPredictions(activeIdentifiers);
            }
        }
    }
    
    // Someone is claiming that some other cells predict us.
    protected void handleSelfPrediction(PredictionMessage message) throws ExecutionException {
        Preconditions.checkArgument(message.getDestination().equals(this.id));
        Preconditions.checkArgument(!message.getActives().contains(this.id));
        
        this.predictionTable.add(message);
    }
    
    protected void run(Message incomingMessage) {
        try {
            switch (incomingMessage.getType()) {
                case ACTIVATION:

                    ActivationMessage msg = (ActivationMessage) incomingMessage;
                    
                    if (VERBOSE) {
                        System.out.println(this.id + " received message: " + msg);
                    }

                    if (incomingMessage.getDestination().equals(this.id)) {
                        this.handleSelfActivation(msg);
                    } else {
                        this.handleOtherActivation(msg);
                    }

                    break;

                case PREDICTION:

                    PredictionMessage pred = (PredictionMessage) incomingMessage;
                    
                    // Someone thinks we predict them 
                    if (incomingMessage.getDestination().equals(this.id)) {
                        if (VERBOSE) {
                            System.out.println(this.id + " received message: " + pred);
                        }
                        this.handleSelfPrediction(pred);
                    } else {
                        // This might not happen any more..
                        throw new IllegalStateException("Blahhh");
                    }
                    break;
            }

        } catch (ExecutionException ee) {
            if (this.isRunning()) {
                ee.printStackTrace();
            }
        }
    }
    
    @Override
    protected final void runOneIteration() throws Exception {
//        while (this.inputQueue.hasMessage()) {
            
            Message incomingMessage = this.inputQueue.get();

            if (incomingMessage != null) {

                try {
                    this.run(incomingMessage);
                } catch (Throwable th) {
                    // Don't let a processing error stop anything
                    // TODO: log
                    if (this.isRunning()) {
                        th.printStackTrace();
                    }
                }
            } else {
                if (this.isRunning()) {
                    System.out.println("BAD - should have blocked on receive");
                }
            }
//        }
    }
    
    @Override
    protected final Scheduler scheduler() {
        return this.scheduler;
    }
    
    private void sendPredictionFeedback() {
//
//	// TBD: Consider that we are giving feedback to specific
//	//      sources individually for there effort in predicting
//	//      us.  However, what we may want instead is to make
//	//      sure several sources predicted us (above a threshold,
//	//      etc.) and then call it a prediction.  I'd like to
//	//      avoid having a narrow prediction mechanism.  One 
//	//      activation to another activation is too narrow, we
//	//      want is one SDR to another SDR.
//
//
//        if (this.isActive) {
//            Map<Identifier,Double> predictionsById = this.getPredictors();
//
//            // TBD: sending prediction feedback to the source as well as a list of
//            //      co-predictive cells.  This would help those active cells project
//            //      predictions into the future.
//
//            if (predictionsById.size() >= PREDICTION_DUTY_MIN) {
//
////                StringBuilder sb = new StringBuilder();
////
////                for (Map.Entry<Identifier, Double> totalBySourceId : predictionsById.entrySet()) {
////
////                    double value = totalBySourceId.getValue();
////                    // this calculation is arbitrary
////                    value = Math.log(value);
////
////                    sb.append(totalBySourceId.getKey());
////                    sb.append("|");
////
////                    PredictionMessage outgoingMessages = new PredictionMessage(this.id, totalBySourceId.getKey(), value, new Date().getTime());
////                    this.router.send(this.id, outgoingMessages);
////                }
////
////                // Just dump the predictions to stdout for now
////                System.out.println(sb.toString() + " => " + this.id);
//            }
//            
//        } else {
//            // give negative feedback
//            
////            Map<Identifier,Double> predictionsById = this.getPredictors();
////            
////            for (Map.Entry<Identifier, Double> totalBySourceId : predictionsById.entrySet()) {
////                // this calculation is arbitrary
////                double value = (totalBySourceId.getValue() / 2) * -1.0;
////                
////                PredictionMessage outgoingMessages = new PredictionMessage(this.id, totalBySourceId.getKey(), value, new Date().getTime());
////                this.router.send(this.id, outgoingMessages);
////            }
//        }
    }

    // for negative feedback,
    // if someone in our prediction table is active and we don't go active
    // then reduce prediction.
    
    public void setNeighbors(Collection<Destination> neighbors) {
        this.router.setDestinations(neighbors);
    }
    
    @Override
    public String toString() {
        return "{" + this.id + ": A_" + this.activeTable + "|P_" + this.predictionTable + "}";
    }
}

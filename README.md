Theoretical AGI memory system as a mesh network.
=====

It's a twist on the HTM CLA.  The hope is that it's more scalable and simpler.  It's really just a fun thought exercise for now.

The basic idea: 

* You have a network of cells
* Each cell listens for activations from remote cells
* If a cell becomes active (from an outside source) it propagates a residual activation to other cells
* If that same active cell was predicted to be active by a different cell, then it sends a prediction feedback message and that prediction becomes stronger
* Right now, the activations and predictions are time-expiring (for better or worse - this is still in flux)

The hope:

* Anomaly detection
* Scalable learning (small local tables leading to a distributed memory mesh, no need for billions of synapses (read: pointers))
* Online learning

What isn't yet:

* There's no conversion from actual outside data to SDR which is presumed to be fed into the mmesh as input activations


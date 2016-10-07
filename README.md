# Combiner Fun

Implements a rendezvous to hand off work from work queues through a SynchronousQueue.
 
## Running Combiner Fun Tests 

1. Clone this repository.
2. from a console cd into repository directory.
3. run Maven clean install

```
mvn clean install
``` 

## Combiner Implementation and Invariants
 
 The a combiner must support the following requirements. Queues may be added and removed at any time. 
 Work from Queues will be processed based on the relative priority for the queue. For example,
 with two queues of priority 1.0 and 9.0 the combiner would hand off ~ 
 9 bits of work for every one bit of work. 
 
 
 ... 

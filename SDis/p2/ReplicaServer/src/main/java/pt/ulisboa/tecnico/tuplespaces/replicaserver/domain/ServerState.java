package pt.ulisboa.tecnico.tuplespaces.replicaserver.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;


public class ServerState {

    boolean DEBUG;
    private List<String> tuples;                            // tuple space
    private boolean lock = false;                           // server lock state for the TAKE operation
    private int lockHolder = -1;                            // client ID holding the lock
    private Queue<Integer> lockQueue = new LinkedList<>();  // queue for pending lock requests


    public ServerState(boolean debug) {
        this.DEBUG = debug;
        this.tuples = new ArrayList<String>();
    }

    /**
     * REQUEST-LOCK operation:  acquires the lock for a client
     *                          if the lock is not available the client is added to the queue
     *                          blocks the client until the lock is granted
     *
     * @param clientId the client ID
     * @return true if the lock is granted to the client
     */
    public boolean acquireLock(int clientId) {
        boolean inQueue = false;

        while (true) {
            synchronized (this) {  // Only synchronize the critical section
                if (!lock) {                        // lock is free when the queue is empty
                    lock = true;                    // acquire the lock
                    lockHolder = clientId;          // set the lock holder
                    if (DEBUG) {
                        System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock granted to client %d\n", clientId);
                    }
                    return true;
                }
                else if (lockHolder == clientId) {  // for when the lock gets passed to the next client in the queue
                    if (DEBUG) {
                        System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock granted to client %d (next in queue)\n", clientId);
                    }
                    return true;
                }
                else {
                    if (!inQueue) {
                        lockQueue.add(clientId);    // add client to the queue only once
                        inQueue = true;
                        if (DEBUG) {
                            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock request from client %d queued!\n", clientId);
                        }
                    }
                    try {
                        wait();                         // block
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * REQUEST-UNLOCK operation:    releases the lock
     */
    public synchronized void freeLock() {
        if (lockQueue.isEmpty()) {
            this.lock = false;
            this.lockHolder = -1;
            // since the Queue is empty, no one is waiting for the lock
            // so we don't need to notify anyone
        }
        else {
            this.lockHolder = lockQueue.poll();
            notifyAll();    // notify the next client in the queue
        }
    }

    /**
     * PUT operation:   adds a tuple to the tuple space
     * @param tuple the tuple to be added
     */
    public synchronized void put(String tuple) {
        this.tuples.add(tuple);

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Added tuple: " + tuple);
        }

        notifyAll();
    }

    /**
     * returns a tuple that matches the pattern that may be a regular expression
     * @param pattern the pattern to match
     * @return the tuple that matches the pattern
     */
    private String getMatchingTuple(String pattern) {
        for (String tuple : this.tuples) {
            if (tuple.matches(pattern)) {
                return tuple;
            }
        }
        return null;
    }

    /**
     * READ operation:  accepts a tuple description to find a match in the tuple space
     *                  blocks the client until there is a tuple that satisfies description
     *                  the tuple is not removed from the tuple space
     *
     * @param pattern the pattern to match
     * @return the tuple that matches the pattern
     */
    public synchronized String read(String pattern) {
        while (true) {
            String t = getMatchingTuple(pattern);
            if (t != null) {
                if (DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Read tuple %s for pattern: %s%n", t, pattern);
                }
                return t;
            }

            if (DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Blocking %s because no tuple found for pattern: %s\n\n", Thread.currentThread().getName(), pattern);
            }

            try {
                wait(); // waits until a tuple is available
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * TAKE operation:  accepts a tuple description to find a match in the tuple space
     *                  blocks the client until there is a tuple that satisfies description
     *                  the tuple is removed from the tuple space
     *
     * @param pattern the pattern to match
     * @return the tuple that matches the pattern
     */
    public synchronized String take(String pattern) {
        while (true) {
            String t = getMatchingTuple(pattern);
            if (t != null) {
                this.tuples.remove(t);

                if (DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Took tuple %s for pattern: %s%n", t, pattern);
                }

                return t;
            }

            if (DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Blocking %s because no tuple found for pattern: %s\n\n", Thread.currentThread().getName(), pattern);
            }

            try {
                wait(); // waits until a tuple is available
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * GET-TUPLE-SPACES-STATE operation: returns the tuple space state of the server
     * @return the tuple space state of the server
     */
    public synchronized List<String> getTupleSpacesState() {

        List<String> tupleSpacesState = new ArrayList<String>(this.tuples);

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Got tuple space state");
        }

        return tupleSpacesState;
    }
}

package pt.ulisboa.tecnico.tuplespaces.replicaserver.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

import java.util.Map;
import java.util.HashMap;


public class ServerState {

    boolean DEBUG;    
    private Map<String, Integer> space;                     // tuple space with mapped lock for the TAKE operation
                                                            // space={"<a,b>": -1, "<c,d>": 1, "<e,f>": -1, "<g,h>": 2, ...}
                                                            // key: tuple, value: N (lock status)
                                                            // N = -1 => the tuple is free
                                                            // N > 0  => the tuple is locked by client N


    public ServerState(boolean debug) {
        this.DEBUG = debug;
        this.space = new HashMap<String, Integer>();
    }

    /**
     * REQUEST-LOCK operation:  tries to acquire the lock(s) for a client
     *
     * @param clientId the client ID
     * @param pattern the pattern to match. the pattern may be a regular expression or a simple tuple
     * @return true if the lock is granted to the client
     */
    public List<String> acquireLock(int clientId, String pattern) {
        boolean hasAtLeastOneMatchLocked = false;

        while (true) {
            synchronized (this) {
                
                List<String> matches = new ArrayList<String>(); // list of tuples that match the pattern
                
                for (Map.Entry<String, Integer> entry : this.space.entrySet()) {    // iterate over the tuple space
                    if (entry.getKey().matches(pattern)) {                              // if the tuple matches the pattern
                        if (entry.getValue() == -1 || entry.getValue() == clientId) {   // if the tuple is free or if the tuple is locked, but it's locked by the client
                            entry.setValue(clientId);       // lock the tuple for the client
                            matches.add(entry.getKey());    // add the tuple to the list of matches
                            if (DEBUG) {
                                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock granted to client %d for tuple %s\n", clientId, entry.getKey());
                            }
                        }
                        else {
                            // if the tuple matches the pattern but is locked by another client
                            // we keep going to check the other tuples
                            hasAtLeastOneMatchLocked = true;
                            if (DEBUG) {
                                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock denied to client %d for tuple %s - locked by client %d\n", clientId, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }

                if (matches.isEmpty() && !hasAtLeastOneMatchLocked) {    // MISS: the client didn't get any locks because there are no matching tuples
                    if (DEBUG) {
                        System.err.printf("[\u001B[34mDEBUG\u001B[0m] MISS: No tuples found for client %d with pattern %s. Block until a tuple is added\n", clientId, pattern);
                    }
                    try {
                        wait();             // block until a tuple is added
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {                      // HIT: the client successfully acquired locks for the tuples OR the client got no locks but it has matching tuples that are locked by other clients
                    if (DEBUG) {
                        System.err.println("[\u001B[34mDEBUG\u001B[0m] HIT: " + this.space);
                    }
                    return matches; // return the list of tuples that match the pattern
                }
            }
        }
    }

    /**
     * REQUEST-UNLOCK operation:    releases the lock(s) for a client
     * 
     * @param clientId the client ID
     */
    public synchronized void freeLock(int clientId) {
        
        if (DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Freeing possible locks for client %d\n", clientId);
        }

        for (Map.Entry<String, Integer> entry : this.space.entrySet()) {    // iterate over the tuple space
            if (entry.getValue() == clientId) {       // if the tuple is locked by the client
                entry.setValue(-1);                   // unlock the tuple for the client
                if (DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Lock released for client %d for tuple %s\n", clientId, entry.getKey());
                }
            }
        }
    }

    /**
     * PUT operation:   adds a tuple to the tuple space
     * @param tuple the tuple to be added
     */
    public synchronized void put(String tuple) {
        this.space.put(tuple, -1);

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
        for (Map.Entry<String, Integer> entry : this.space.entrySet()) {
            if (entry.getKey().matches(pattern)) { return entry.getKey(); }
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
                this.space.remove(t);

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

        List<String> tupleSpacesState = new ArrayList<String>(this.space.keySet());

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Got tuple space state");
        }

        return tupleSpacesState;
    }
}

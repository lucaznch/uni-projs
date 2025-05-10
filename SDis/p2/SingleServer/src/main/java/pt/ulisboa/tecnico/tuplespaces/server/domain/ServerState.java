package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

    boolean DEBUG;
    private List<String> tuples;    // tuple space

    public ServerState(boolean debug) {
        this.DEBUG = debug;
        this.tuples = new ArrayList<String>();
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

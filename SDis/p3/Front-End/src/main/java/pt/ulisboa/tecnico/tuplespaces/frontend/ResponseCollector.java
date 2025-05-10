package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;
import java.util.List;


/**
 * the ResponseCollector class is responsible for collecting the responses from the TupleSpaces servers
 * it collects the responses from the servers through the observers
 */
public class ResponseCollector {

    private ArrayList<ResponseEntry> collectedHistory;          // table with all the PUT/READ/TAKE responses received from the servers
                                                                // | requestType | requestId | request | response | serverId |
                                                                // |     PUT     |     1     |   ...   |   ...    |    0     |
                                                                // |     PUT     |     1     |   ...   |   ...    |    1     |
                                                                // |     PUT     |     1     |   ...   |   ...    |    2     |

    private ArrayList<ResponseLockEntry> collectedLockHistory;  // table with all the LOCK/UNLOCK responses received from the servers
                                                                // | requestType | requestId | request | response | serverId | retryId |
                                                                // |     LOCK    |     1     |   ...   |   ...    |    0     |    0    |
                                                                // |     LOCK    |     1     |   ...   |   ...    |    2     |    0    |
                                                                // |     LOCK    |     1     |   ...   |   ...    |    0     |    1    |
                                                                // |     LOCK    |     1     |   ...   |   ...    |    2     |    1    |
                                                                // |    UNLOCK   |     1     |   ...   |   ...    |    0     |    1    |
                                                                // |    UNLOCK   |     1     |   ...   |   ...    |    2     |    1    |

    private ArrayList<ResponseGetEntry> collectedGetHistory;    // table with all the GET responses received from the servers
                                                                // | requestId |    response    | serverId |
                                                                // |     1     | [<1,2>, <3,4>] |    0     |
                                                                // |     1     | [<1,2>, <3,4>] |    1     |
                                                                // |     1     | [<1,2>, <3,4>] |    2     |


    public ResponseCollector() {
        this.collectedHistory = new ArrayList<ResponseEntry>();
        this.collectedLockHistory = new ArrayList<ResponseLockEntry>();
        this.collectedGetHistory = new ArrayList<ResponseGetEntry>();
    }


    /**
     * this method is used to add a response to the collected history
     * @param requestType the type of the request (PUT, READ, TAKE)
     * @param requestId the request ID
     * @param request the request
     * @param response the response
     * @param serverId the server ID
     */
    public synchronized void addResponse(String requestType, int requestId, String request, String response, int serverId) {
        this.collectedHistory.add(new ResponseEntry(requestType, requestId, request, response, serverId));
        notifyAll();
    }

    /**
     * this method is used to get the response for a given request ID
     * NOTE: requests IDs are unique! in the table, there are 3 entries for each request ID, one for each server
     * 
     * @param requestId the request ID
     * @param requestType the type of the request (PUT, READ, TAKE)
     * @return the response for the given request ID
     */
    public synchronized String getResponse(int requestId, String requestType) {
        if (requestType.equals("PUT")) {
            int requestsCounter = 0;
            int okCounter = 0;

            for (ResponseEntry e : this.collectedHistory) {
                if (e.getRequestId() == requestId) {
                    // System.err.println("[\u001B[34mDEBUG\u001B[0m] entry = " + e.toString());
                    requestsCounter++;
                    if (e.getResponse().equals("OK")) { okCounter++; }
                    if (requestsCounter == 3) { break; }
                }
            }
            if (okCounter == 3) { return "OK"; }
            else { return "NO"; }
        }
        else if (requestType.equals("READ")) {
            for (ResponseEntry e : this.collectedHistory) {     // READ waits only for 1 response
                if (e.getRequestId() == requestId) {            // therefore we get the first response we find
                    return e.getResponse();
                }
            }
        }
        else if (requestType.equals("TAKE")) {                  // TAKE waits for 3 responses
            for (ResponseEntry e : this.collectedHistory) {     // but with Maekawa algorithm we can get consistency between servers
                if (e.getRequestId() == requestId) {            // therefore we get the first response we find
                    return e.getResponse();
                }
            }
        }

        return "NO";
    }

    /**
     * this method is used to wait until N the responses for a given request ID are received.
     * @param requestId the request ID to wait for
     * @param n the number of responses to wait for
     */
    public synchronized void waitUntilAllReceived(int requestId, int n) {
        int requestsCounter;

        while (true) {
            requestsCounter = 0;

            if (n == 3) {
                for (ResponseEntry e : this.collectedHistory) {
                    if (e.getRequestId() == requestId) {
                        requestsCounter++;
                        if (requestsCounter == 3) { return; }
                    }
                }
                
                try { wait(); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            else if (n == 1) {
                for (ResponseEntry e : this.collectedHistory) {
                    if (e.getRequestId() == requestId) {
                        return;
                    }
                }

                try { wait(); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * this method is used to add a lock response to the collected history
     * @param requestType the type of the request (LOCK, UNLOCK)
     * @param requestId the request ID
     * @param request the request
     * @param response the response
     * @param serverId the server ID
     */
    public synchronized void addLockResponse(String requestType, int requestId, String request, List<String> response, int serverId, int retryId) {
        this.collectedLockHistory.add(new ResponseLockEntry(requestType, requestId, request, response, serverId, retryId));
        notifyAll();
    }

    /**
     * this method is used to get the lock response for a given request ID
     * NOTE: requests IDs are unique! in the table, there are 2 entries for each request ID, one for each server
     * 
     * @param requestId the request ID
     * @param serverId the server ID
     * @return the lock response for the given request ID
     */
    public synchronized List<String> getLockResponse(int requestId, int serverId, int retryId) {
        for (ResponseLockEntry e : this.collectedLockHistory) {
            if (e.getRequestId() == requestId && e.getServerId() == serverId && e.getRetryId() == retryId) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] entry = " + e.toString());
                return e.getResponse();
            }
        }
        return null;
    }

    /**
     * this method is used to wait until all the lock responses for a given request ID and retry ID are received.
     * @param requestId the request ID to wait for
     * @param retryId the retry ID to wait for
     * @param requestType the type of the request (LOCK, UNLOCK)
     */
    public synchronized void waitUntilAllLockReceived(int requestId, int retryId, String requestType) {
        int requestsCounter;

        while (true) {
            requestsCounter = 0;

            for (ResponseLockEntry e : this.collectedLockHistory) {
                if (e.getRequestId() == requestId && e.getRetryId() == retryId && e.getRequestType().equals(requestType)) {
                    requestsCounter++;
                    if (requestsCounter == 2) { return; }
                }
            }
            
            try { wait(); }
            catch (InterruptedException e) { e.printStackTrace(); }
            
        }
    }

    /**
     * this method is used to add a get response to the collected history
     * @param requestId the request ID
     * @param response the response
     * @param serverId the server ID
     */
    public synchronized void addGetResponse(int requestId, List<String> response, int serverId) {
        this.collectedGetHistory.add(new ResponseGetEntry(requestId, response, serverId));
        notifyAll();
    }

    /**
     * this method is used to get tuple spaces from each server and combine them into a single response
     * NOTE: requests IDs are unique! in the table, there are 3 entries for each request ID, one for each server
     * 
     * @param requestId the request ID
     * @return the combined response with the tuple spaces from each server
     */
    public synchronized List<String> getGetResponse(int requestId) {
        List<String> combinedResponse = new ArrayList<>();
        
        for (ResponseGetEntry e : this.collectedGetHistory) {
            if (e.getRequestId() == requestId) {
                combinedResponse.addAll(e.getResponse());
            }
        }
        
        return combinedResponse;
    }

    /**
     * this method is used to wait until all the get responses for a given request ID are received.
     * @param requestId the request ID to wait for
     */
    public synchronized void waitUntilAllGetReceived(int requestId) {
        int requestsCounter;

        while (true) {
            requestsCounter = 0;

            for (ResponseGetEntry e : this.collectedGetHistory) {
                if (e.getRequestId() == requestId) {
                    requestsCounter++;
                    if (requestsCounter == 3) { return; }
                }
            }
            
            try { wait(); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}


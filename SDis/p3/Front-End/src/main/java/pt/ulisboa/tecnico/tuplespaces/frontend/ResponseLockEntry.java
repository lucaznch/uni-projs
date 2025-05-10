package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.List;

public class ResponseLockEntry {
    private final String requestType;
    private final int requestId;
    private final String request;
    private final List<String> response;
    private final int serverId;
    private final int retryId;

    public ResponseLockEntry(String requestType, int requestId, String request, List<String> response, int serverId, int retryId) {
        this.requestType = requestType;
        this.requestId = requestId;
        this.request = request;
        this.response = response;
        this.serverId = serverId;
        this.retryId = retryId;
    }

    public String getRequestType() { return requestType; }

    public int getRequestId() { return requestId; }

    public String getRequest() { return request; }

    public List<String> getResponse() { return response; }

    public int getServerId() { return serverId; }

    public int getRetryId() { return retryId; }

    @Override
    public String toString() {
        return String.format("(%s, %d, %s, %s, %d, %d)", requestType, requestId, request, response, serverId, retryId);
    }
}














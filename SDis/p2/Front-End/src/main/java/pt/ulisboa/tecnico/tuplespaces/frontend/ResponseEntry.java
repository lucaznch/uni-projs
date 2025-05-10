package pt.ulisboa.tecnico.tuplespaces.frontend;

public class ResponseEntry {
    private final String requestType;
    private final int requestId;
    private final String request;
    private final String response;
    private final int serverId;

    public ResponseEntry(String requestType, int requestId, String request, String response, int serverId) {
        this.requestType = requestType;
        this.requestId = requestId;
        this.request = request;
        this.response = response;
        this.serverId = serverId;
    }

    public String getRequestType() { return requestType; }

    public int getRequestId() { return requestId; }

    public String getRequest() { return request; }

    public String getResponse() { return response; }

    public int getServerId() { return serverId; }

    @Override
    public String toString() {
        return String.format("(%s, %d, %s, %s, %d)", requestType, requestId, request, response, serverId);
    }
}














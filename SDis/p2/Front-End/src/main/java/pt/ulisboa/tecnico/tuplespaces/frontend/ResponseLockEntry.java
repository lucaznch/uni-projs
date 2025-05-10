package pt.ulisboa.tecnico.tuplespaces.frontend;

public class ResponseLockEntry {
    private final String requestType;
    private final int requestId;
    private final String request;
    private final boolean response;
    private final int serverId;

    public ResponseLockEntry(String requestType, int requestId, String request, boolean response, int serverId) {
        this.requestType = requestType;
        this.requestId = requestId;
        this.request = request;
        this.response = response;
        this.serverId = serverId;
    }

    public String getRequestType() { return requestType; }

    public int getRequestId() { return requestId; }

    public String getRequest() { return request; }

    public boolean getResponse() { return response; }

    public int getServerId() { return serverId; }
}














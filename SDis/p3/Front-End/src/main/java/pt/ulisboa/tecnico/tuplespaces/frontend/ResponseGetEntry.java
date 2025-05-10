package pt.ulisboa.tecnico.tuplespaces.frontend;

import java.util.ArrayList;
import java.util.List;


public class ResponseGetEntry {
    private final int requestId;
    private final List<String> response;
    private final int serverId;


    public ResponseGetEntry(int requestId, List<String> response, int serverId) {
        this.requestId = requestId;
        this.response = response;
        this.serverId = serverId;
    }

    public int getRequestId() { return requestId; }

    public List<String> getResponse() { return response; }

    public int getServerId() { return serverId; }
}





package pt.ulisboa.tecnico.tuplespaces.frontend;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * the FrontendGetTupleSpacesStateObserver class is responsible for handling the responses from a TupleSpaces server
 * it handles the asynchronous responses from the server and forwards them to the ResponseCollector
 * 
 */
public class FrontendGetTupleSpacesStateObserver implements StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> {
    private final int serverId;
    private final int requestId;
    private ResponseCollector collector;

    public FrontendGetTupleSpacesStateObserver(int serverId, int requestId, ResponseCollector c) {
        this.serverId = serverId;
        this.requestId = requestId;
        this.collector = c;
    }

    /**
     * this method is called when a GET response is received from the server
     * it forwards the response to the ResponseCollector
     * 
     * @param response the response received from the server
     */
    @Override
    public void onNext(TupleSpacesOuterClass.getTupleSpacesStateResponse response) {
        int len = response.getTupleCount();
        List<String> space = new ArrayList<String>();
        for (int i = 0; i < len; i++) { space.add(response.getTuple(i)); }

        collector.addGetResponse(this.requestId, space, this.serverId);
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("GET error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        // System.out.println("[\u001B[34mDEBUG\u001B[0m] FrontendGetObserver: GET completed");
    }
}


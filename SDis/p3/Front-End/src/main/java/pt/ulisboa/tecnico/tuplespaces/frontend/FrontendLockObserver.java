package pt.ulisboa.tecnico.tuplespaces.frontend;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.ArrayList;


public class FrontendLockObserver implements StreamObserver<TupleSpacesOuterClass.LockResponse> {
    private final int serverId;
    private final int requestId;
    private final String request;
    private final int retryId;
    private ResponseCollector collector;

    public FrontendLockObserver(int serverId, int requestId, String request, int retryId, ResponseCollector c) {
        this.serverId = serverId;
        this.requestId = requestId;
        this.request = request;
        this.retryId = retryId;
        this.collector = c;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.LockResponse response) {
        int len = response.getMatchCount();
        List<String> matches = new ArrayList<String>();
        for (int i = 0; i < len; i++) { matches.add(response.getMatch(i)); }

        collector.addLockResponse("LOCK", this.requestId, this.request, matches, this.serverId, this.retryId);
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("LOCK error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        //System.out.printf("[\u001B[34mDEBUG\u001B[0m] FrontendLockObserver: LOCK completed (#%d)", this.requestId);
    }
}






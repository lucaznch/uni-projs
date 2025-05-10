package pt.ulisboa.tecnico.tuplespaces.frontend;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.stub.StreamObserver;



public class FrontendLockObserver implements StreamObserver<TupleSpacesOuterClass.LockResponse> {
    private final int serverId;
    private final int requestId;
    private final String request;
    private ResponseCollector collector;

    public FrontendLockObserver(int serverId, int requestId, String request, ResponseCollector c) {
        this.serverId = serverId;
        this.requestId = requestId;
        this.request = request;
        this.collector = c;
    }

    @Override
    public void onNext(TupleSpacesOuterClass.LockResponse response) {
        collector.addLockResponse("LOCK", this.requestId, this.request, response.getGranted(), this.serverId);
        // System.err.printf("[\u001B[34mDEBUG\u001B[0m] FrontendLockObserver: received LOCK response (#%d) from server %d: %s\n", this.requestId, this.serverId, response.getGranted());
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






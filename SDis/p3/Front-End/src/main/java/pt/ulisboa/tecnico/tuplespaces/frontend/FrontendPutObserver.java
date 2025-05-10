package pt.ulisboa.tecnico.tuplespaces.frontend;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.stub.StreamObserver;



/**
 * the FrontendObserver class is responsible for handling the responses from a TupleSpaces server
 * it handles the asynchronous responses from the serversand forwards them to the ResponseCollector
 * 
 */
public class FrontendPutObserver implements StreamObserver<TupleSpacesOuterClass.PutResponse> {
                                            // this observer is associated with a specifc:
    private final int serverId;             // server
    private final int requestId;            // request
    private final String request;           // request sent to the server
    private ResponseCollector collector;

    public FrontendPutObserver(int serverId, int requestId, String request, ResponseCollector c) {
        this.serverId = serverId;
        this.requestId = requestId;
        this.request = request;
        this.collector = c;
    }

    /**
     * this method is called when a PUT response is received from the server
     * it forwards the response to the ResponseCollector
     * 
     * @param response the response received from the server
     */
    @Override
    public void onNext(TupleSpacesOuterClass.PutResponse response) {        
        collector.addResponse("PUT", this.requestId, this.request, response.getOk(), this.serverId);
        // System.err.printf("[\u001B[34mDEBUG\u001B[0m] FrontendPutObserver: received PUT response from server %d: %s\n", this.serverId, response.getOk());
    }

    /**
     * this method is called when an error occurs in the communication with the server
     * 
     * @param t the error that occurred
     */
    @Override
    public void onError(Throwable t) {
        System.out.println("PUT error: " + t.getMessage());
    }

    /**
     * this method is called when the communication with the server is completed
     */
    @Override
    public void onCompleted() {
        // System.out.println("[\u001B[34mDEBUG\u001B[0m] FrontendPutObserver: PUT completed");
    }
}







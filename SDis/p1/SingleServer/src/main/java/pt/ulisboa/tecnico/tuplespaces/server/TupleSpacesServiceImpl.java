package pt.ulisboa.tecnico.tuplespaces.server;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.List;

import io.grpc.stub.StreamObserver;


/**
 * TupleSpacesServiceImpl is the class that implements the gRPC service.
 * It extends the TupleSpacesGrpc.TupleSpacesImplBase class that was generated from the proto file.
 * It overrides the methods defined in the proto file.
 * It receives the requests from the clients and processes them.
 * It uses the ServerState class to store the tuple space and perform the operations.
 */
public class TupleSpacesServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private boolean DEBUG;
    private int numberPutRequests = 1;
    private int numberReadRequests = 1;
    private int numberTakeRequests = 1;
    private int numberGetTupleSpacesStateRequests = 1;
    private ServerState serverState;

    public TupleSpacesServiceImpl(boolean debug) {
        this.DEBUG = debug;
        this.serverState = new ServerState(debug); 
    }

    @Override
    public void put(TupleSpacesOuterClass.PutRequest request, StreamObserver<TupleSpacesOuterClass.PutResponse> responseObserver) {
        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server received PUT request (#%d) in %s, %s", this.numberPutRequests, Thread.currentThread().getName(), request);
        }

        this.serverState.put(request.getNewTuple());                // add tuple to tuple space
    
        TupleSpacesOuterClass.PutResponse response = 
            TupleSpacesOuterClass.PutResponse.newBuilder().build(); // construct a new Protobuffer object to send as response

        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server sending PUT response (#%d) in %s\n\n", this.numberPutRequests++, Thread.currentThread().getName());
        }
        responseObserver.onNext(response);                          // use the responseObserver to send the response
        responseObserver.onCompleted();                             // after sending the response, complete the call
    }

    @Override
    public void read(TupleSpacesOuterClass.ReadRequest request, StreamObserver<TupleSpacesOuterClass.ReadResponse> responseObserver) {
        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server received READ request (#%d) in %s, %s", this.numberReadRequests, Thread.currentThread().getName(), request);
        }

        String tuple = this.serverState
                            .read(request.getSearchPattern());      // read tuple from tuple space

        TupleSpacesOuterClass.ReadResponse response = 
            TupleSpacesOuterClass.ReadResponse
                                .newBuilder()
                                .setResult(tuple)
                                .build();                           // construct a new Protobuffer object to send as response

        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server sending READ response (#%d) in %s, result: %s\n\n", this.numberReadRequests++, Thread.currentThread().getName(), tuple);
        }
        responseObserver.onNext(response);                          // use the responseObserver to send the response
        responseObserver.onCompleted();                             // after sending the response, complete the call
    }

    @Override
    public void take(TupleSpacesOuterClass.TakeRequest request, StreamObserver<TupleSpacesOuterClass.TakeResponse> responseObserver) {
        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server received TAKE request (#%d) in %s, %s", this.numberTakeRequests, Thread.currentThread().getName(), request);
        }

        String tuple = this.serverState
                            .take(request.getSearchPattern());      // take tuple from tuple space

        TupleSpacesOuterClass.TakeResponse response = 
            TupleSpacesOuterClass.TakeResponse
                                .newBuilder()
                                .setResult(tuple)
                                .build();                           // construct a new Protobuffer object to send as response

        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server sending TAKE response (#%d) in %s, result: %s\n\n", this.numberTakeRequests++, Thread.currentThread().getName(), tuple);
        }

        responseObserver.onNext(response);                          // use the responseObserver to send the response
        responseObserver.onCompleted();                             // after sending the response, complete the call
    }

    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> responseObserver) {
        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server received GET-TUPLE-SPACES-STATE request (#%d) in %s%n", this.numberGetTupleSpacesStateRequests, Thread.currentThread().getName());
        }

        List<String> tupleSpacesState = this.serverState
                                            .getTupleSpacesState(); // get tuple space state

        TupleSpacesOuterClass.getTupleSpacesStateResponse.Builder responseBuilder = 
                TupleSpacesOuterClass.getTupleSpacesStateResponse.
                                    newBuilder();                   // create a response builder object to build the response

        for (String tuple : tupleSpacesState) {
            responseBuilder.addTuple(tuple);                        // keep adding tuples to response
        }

        TupleSpacesOuterClass.getTupleSpacesStateResponse response = responseBuilder.build();   // build the response

        if (this.DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Server sending GET-TUPLE-SPACES-STATE response (#%d) in %s, result %s\n\n", this.numberGetTupleSpacesStateRequests++, Thread.currentThread().getName(), tupleSpacesState.toString());
        }

        responseObserver.onNext(response);                          // use the responseObserver to send the response
        responseObserver.onCompleted();                             // after sending the response, complete the call
    }
}


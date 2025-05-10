package pt.ulisboa.tecnico.tuplespaces.frontend;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.stub.StreamObserver;     // StreamObserver is used to send responses to the Client
import io.grpc.ManagedChannel;          // ManagedChannel is used to create a channel to the Server
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.util.ArrayList;
import java.util.List;


public class FrontendImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private boolean DEBUG;
    private int requestId = 1;
    private final int numServers;
    private final ResponseCollector collector;              // frontend(client): collector is responsible for collecting the responses from the TupleSpaces servers associated with a request
    private final ManagedChannel[] channels;                // frontend(client): channels is the abstraction to connect to the server endpoints
    private final TupleSpacesGrpc.TupleSpacesStub[] stubs;  // frontend(client): stubs are used to make remote calls to the server. 
                                                            // frontend(client) will use non-blocking stubs to make remote calls to the server
    private final Metadata.Key<String> CUSTOM_HEADER_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);


    public FrontendImpl(boolean debug, int numServers, String[] servers) {
        this.DEBUG = debug;
        this.numServers = numServers;
        this.collector = new ResponseCollector();
        this.channels = new ManagedChannel[numServers];
        this.stubs = new TupleSpacesGrpc.TupleSpacesStub[numServers];

        for (int i = 0; i < numServers; i++) {
            this.channels[i] = ManagedChannelBuilder.forTarget(servers[i])
                                                    .usePlaintext()
                                                    .build();       // create a channel to each server
            this.stubs[i] = TupleSpacesGrpc.newStub(channels[i]);   // create non-blocking stub to make remote calls to each server
        }
    }


    /**
     * this method is called when a PUT request is received from the client
     * it forwards the request to the server and sends the response back to the client
     * 
     * @param clientRequest the request received from the client
     * @param clientResponseObserver the observer to send the response back to the client
     */
    @Override
    public void put(TupleSpacesOuterClass.PutRequest clientRequest, StreamObserver<TupleSpacesOuterClass.PutResponse> clientResponseObserver) {
        String tuple = clientRequest.getNewTuple();                                 // get the tuple from the request sent by the CLIENT
        String headerValue = HeaderServerInterceptor.HEADER_VALUE_CONTEXT_KEY.get();// get the header value from the context

        TupleSpacesOuterClass.PutRequest serverRequest = 
                                TupleSpacesOuterClass.PutRequest
                                                    .newBuilder()
                                                    .setNewTuple(tuple)
                                                    .build();   // construct a new Protobuffer object to send as request to the SERVER

        int currentRequestId;
        synchronized (this) {
            currentRequestId = this.requestId;
            this.requestId++;
        }

        if (this.DEBUG) {
            if (headerValue != null) {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received PUT request (#%d) from client in %s, with delay (%s), %s", currentRequestId, Thread.currentThread().getName(), headerValue, clientRequest);
            }
            else {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received PUT request (#%d) from client in %s, %s", currentRequestId, Thread.currentThread().getName(), clientRequest);
            }
        }

        try {
            for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to every server
                
                if (headerValue != null) {
                    String[] delays = headerValue.split(" ");

                    Metadata metadata = new Metadata();
                    metadata.put(CUSTOM_HEADER_KEY, delays[i]);
                    this.stubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).put(serverRequest, new FrontendPutObserver(i, currentRequestId, tuple, this.collector));
                }
                else {                
                    this.stubs[i].put(serverRequest, new FrontendPutObserver(i, currentRequestId, tuple, this.collector));
                }
                
                if (this.DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent PUT request (#%d) to server %d\n", currentRequestId, i);
                }
            }

            this.collector.waitUntilAllReceived(currentRequestId, 3); // wait until all servers respond

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received PUT responses (#%d) from all servers\n", currentRequestId);
            }

            String result = this.collector.getResponse(currentRequestId, "PUT");

            TupleSpacesOuterClass.PutResponse clientResponse =
                                TupleSpacesOuterClass.PutResponse
                                                    .newBuilder()
                                                    .setOk(result)
                                                    .build();   // construct a new Protobuffer object to send as response to the CLIENT

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sending PUT response (#%d) back to client\n\n", currentRequestId);
            }

            clientResponseObserver.onNext(clientResponse);      // use the responseObserver to send the response
            clientResponseObserver.onCompleted();               // after sending the response, complete the call
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Frontend PUT request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
        }
    }

    /**
     * this method is called when a READ request is received from the client
     * it forwards the request to the server and sends the response back to the client
     * 
     * @param clientRequest the request received from the client
     * @param clientResponseObserver the observer to send the response back to the client
     */
    @Override
    public void read(TupleSpacesOuterClass.ReadRequest clientRequest, StreamObserver<TupleSpacesOuterClass.ReadResponse> clientResponseObserver) {
        String searchPattern = clientRequest.getSearchPattern();// get the search pattern from the request sent by the CLIENT
        String headerValue = HeaderServerInterceptor.HEADER_VALUE_CONTEXT_KEY.get();// get the header value from the context

        TupleSpacesOuterClass.ReadRequest serverRequest =
                                TupleSpacesOuterClass.ReadRequest
                                                    .newBuilder()
                                                    .setSearchPattern(searchPattern)
                                                    .build();   // construct a new Protobuffer object to send as request to the SERVER

        int currentRequestId;
        synchronized (this) {
            currentRequestId = this.requestId;
            this.requestId++;
        }

        if (this.DEBUG) {
            if (headerValue != null) {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received READ request (#%d) from client in %s, with delay (%s), %s", currentRequestId, Thread.currentThread().getName(), headerValue, clientRequest);
            }
            else {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received READ request (#%d) from client in %s, %s", currentRequestId, Thread.currentThread().getName(), clientRequest);
            }
        }

        try {
            for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to every server
                
                if (headerValue != null) {
                    String[] delays = headerValue.split(" ");

                    Metadata metadata = new Metadata();
                    metadata.put(CUSTOM_HEADER_KEY, delays[i]);
                    this.stubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).read(serverRequest, new FrontendReadObserver(i, currentRequestId, searchPattern, this.collector));
                }
                else {                
                    this.stubs[i].read(serverRequest, new FrontendReadObserver(i, currentRequestId, searchPattern, this.collector));
                }

                if (this.DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent READ request (#%d) to server %d\n", currentRequestId, i);
                }
            }

            this.collector.waitUntilAllReceived(currentRequestId, 1); // wait until the first server responds

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received READ response (#%d) from one server\n", currentRequestId);
            }

            String result = this.collector.getResponse(currentRequestId, "READ");

            TupleSpacesOuterClass.ReadResponse clientResponse = 
                                TupleSpacesOuterClass.ReadResponse
                                                    .newBuilder()
                                                    .setResult(result)
                                                    .build();   // construct a new Protobuffer object to send as response to the CLIENT

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sending READ response (#%d) back to client\n\n", currentRequestId);
            }

            clientResponseObserver.onNext(clientResponse);      // use the responseObserver to send the response
            
            /**
             * before completing the call, onComplete(), we need to handle the two remaining responses from the servers
             * two choices:
             * 1. cancel the two remaining observers
             * 2. wait for the two remaining responses
             */
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Frontend waiting for the two remaining responses...");
            }
            this.collector.waitUntilAllReceived(currentRequestId, 3);       // wait until all servers respond
            // result = this.collector.getResponse(currentRequestId, "PUT");   // for debugging purposes only: print the responses from the 3 servers
            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received READ responses (#%d) from remaining servers\n", currentRequestId);
            }
            clientResponseObserver.onCompleted();               // after sending the response, complete the call
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Frontend READ request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
        }
    }

    /**
     * this method is called when a TAKE request is received from the client
     * it forwards the request to the server and sends the response back to the client
     * 
     * @param clientRequest the request received from the client
     * @param clientResponseObserver the observer to send the response back to the client
     */
    @Override
    public void take(TupleSpacesOuterClass.TakeRequest clientRequest, StreamObserver<TupleSpacesOuterClass.TakeResponse> clientResponseObserver) {
        int clientId = clientRequest.getClientId();             // get the client id from the request sent by the CLIENT
        String searchPattern = clientRequest.getSearchPattern();// get the search pattern from the request sent by the CLIENT
        String headerValue = HeaderServerInterceptor.HEADER_VALUE_CONTEXT_KEY.get();// get the header value from the context

        TupleSpacesOuterClass.TakeRequest serverRequest =
                                TupleSpacesOuterClass.TakeRequest
                                                    .newBuilder()
                                                    .setSearchPattern(searchPattern)
                                                    .build();   // construct a new Protobuffer object to send as request to the SERVER

        int currentRequestId;
        synchronized (this) {
            currentRequestId = this.requestId;
            this.requestId++;
        }

        if (this.DEBUG) {
            if (headerValue != null) {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received TAKE request (#%d) from client in %s, with delay (%s), %s", currentRequestId, Thread.currentThread().getName(), headerValue, clientRequest);
            }
            else {
                System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received \u001B[31mTAKE\u001B[0m request (#%d) from client in %s, %s", currentRequestId, Thread.currentThread().getName(), clientRequest);
            }
        }

        // compute voter set
        int voterOne = clientId % 3;
        int voterTwo = (clientId + 1) % 3;
        if (DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Voter set computed (#%d) - server%d and server%d\n", currentRequestId, voterOne, voterTwo);
        }

        TupleSpacesOuterClass.LockRequest lockRequest = 
                                TupleSpacesOuterClass.LockRequest
                                                    .newBuilder()
                                                    .setClientId(clientId)
                                                    .build();   // construct a new Protobuffer object to send as request to the SERVER

        // phase 1: acquire the locks
        try {
            for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to the two servers in voter set
                if (i == voterOne || i == voterTwo) {
                    this.stubs[i].requestLock(lockRequest, new FrontendLockObserver(i, currentRequestId, searchPattern, this.collector));
                    if (this.DEBUG) {
                        System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent LOCK request (#%d) to server %d\n", currentRequestId, i);
                    }
                }
            }

            this.collector.waitUntilAllLockReceived(currentRequestId, "LOCK");

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received LOCK responses (#%d) from both servers\n", currentRequestId);
            }
        }
        catch (StatusRuntimeException e) {
            e.printStackTrace();
        }

        // phase 2: execute the operation and release the locks
        try {
            for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to every server to execute the operation
                
                if (headerValue != null) {
                    String[] delays = headerValue.split(" ");

                    Metadata metadata = new Metadata();
                    metadata.put(CUSTOM_HEADER_KEY, delays[i]);
                    this.stubs[i].withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).take(serverRequest, new FrontendTakeObserver(i, currentRequestId, searchPattern, this.collector));
                }
                else {                
                    this.stubs[i].take(serverRequest, new FrontendTakeObserver(i, currentRequestId, searchPattern, this.collector));
                }
                
                if (this.DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent TAKE request (#%d) to server %d\n", currentRequestId, i);
                }
            }

            this.collector.waitUntilAllReceived(currentRequestId, 3); // wait until all servers respond

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received TAKE responses (#%d) from all servers\n", currentRequestId);
            }

            String result = this.collector.getResponse(currentRequestId, "TAKE");

            TupleSpacesOuterClass.TakeResponse clientResponse = 
                                TupleSpacesOuterClass.TakeResponse
                                                    .newBuilder()
                                                    .setResult(result)
                                                    .build();   // construct a new Protobuffer object to send as response to the CLIENT


            clientResponseObserver.onNext(clientResponse);      // use the responseObserver to send the response to the CLIENT
            
            TupleSpacesOuterClass.UnlockRequest unlockRequest = 
                                    TupleSpacesOuterClass.UnlockRequest
                                                        .newBuilder()
                                                        .setClientId(clientId)
                                                        .build();   // construct a new Protobuffer object to send as request to the SERVER

            // release the locks
            try {
                for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to the two servers in voter set
                    if (i == voterOne || i == voterTwo) {
                        this.stubs[i].releaseLock(unlockRequest, new FrontendUnlockObserver(i, currentRequestId, searchPattern, this.collector));
                        if (this.DEBUG) {
                            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent UNLOCK request (#%d) to server %d\n", currentRequestId, i);
                        }
                    }
                }

                this.collector.waitUntilAllLockReceived(currentRequestId, "UNLOCK");

                if (this.DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received UNLOCK responses (#%d) from both servers\n", currentRequestId);
                }
            }
            catch (StatusRuntimeException e) {
                e.printStackTrace();
            }
            
            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sending TAKE response (#%d) back to client\n", currentRequestId);
            }

            clientResponseObserver.onCompleted();               // after sending the response, complete the call
        }
        catch (StatusRuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * this method is called when a GET-TUPLE-SPACES-STATE request is received from the client
     * it forwards the request to the server and sends the response back to the client
     * 
     * @param clientRequest the request received from the client
     * @param clientResponseObserver the observer to send the response back to the client
     */
    @Override
    public void getTupleSpacesState(TupleSpacesOuterClass.getTupleSpacesStateRequest clientRequest, StreamObserver<TupleSpacesOuterClass.getTupleSpacesStateResponse> clientResponseObserver) {
        TupleSpacesOuterClass.getTupleSpacesStateRequest serverRequest = 
                                TupleSpacesOuterClass.getTupleSpacesStateRequest
                                                    .newBuilder()
                                                    .build();   // construct a new Protobuffer object to send as request to the SERVER

        int currentRequestId;
        synchronized (this) {
            currentRequestId = this.requestId;
            this.requestId++;
        }

        if (this.DEBUG) {
            System.err.printf("\n[\u001B[34mDEBUG\u001B[0m] Frontend received GET-TUPLE-SPACES-STATE request (#%d) from client in %s\n", currentRequestId, Thread.currentThread().getName());
        }

        try {
            for (int i = 0; i < this.numServers; i++) { // make async calls sending the request to every server
                this.stubs[i].getTupleSpacesState(serverRequest, new FrontendGetTupleSpacesStateObserver(i, currentRequestId, this.collector));
                if (this.DEBUG) {
                    System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sent GET-TUPLE-SPACES-STATE request (#%d) to server %d\n", currentRequestId, i);
                }
            }

            this.collector.waitUntilAllGetReceived(currentRequestId); // wait until all servers respond

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend received GET-TUPLE-SPACES-STATE responses (#%d) from all servers\n", currentRequestId);
            }

            List<String> spaces = this.collector.getGetResponse(currentRequestId);

            TupleSpacesOuterClass.getTupleSpacesStateResponse.Builder clientResponseBuilder =
                                TupleSpacesOuterClass.getTupleSpacesStateResponse.newBuilder();  // create a response builder object to build the response

            for (String tuple : spaces) { clientResponseBuilder.addTuple(tuple); }

            TupleSpacesOuterClass.getTupleSpacesStateResponse clientResponse = clientResponseBuilder.build();   // construct a new Protobuffer object to send as response to the CLIENT

            if (this.DEBUG) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend sending GET-TUPLE-SPACES-STATE response (#%d) back to client\n\n", currentRequestId);
            }

            clientResponseObserver.onNext(clientResponse);      // use the responseObserver to send the response
            clientResponseObserver.onCompleted();               // after sending the response, complete the call
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Frontend GET-TUPLE-SPACES-STATE request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
        }
    }
}


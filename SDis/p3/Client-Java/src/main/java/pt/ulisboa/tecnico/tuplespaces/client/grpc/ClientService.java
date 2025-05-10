package pt.ulisboa.tecnico.tuplespaces.client.grpc;

// classes generated from the proto file
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicated.contract.TupleSpacesOuterClass;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.util.ArrayList;
import java.util.List;


public class ClientService {

    private final boolean DEBUG;
    private final int client_id;
    private final String host_port;
    private final ManagedChannel channel;
    private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private final Metadata.Key<String> CUSTOM_HEADER_KEY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    public ClientService(String host_port, int client_id, boolean debug) {
        this.DEBUG = debug;
        this.client_id = client_id;
        this.host_port = host_port;
        this.channel = 
            ManagedChannelBuilder.forTarget(host_port)
                                    .usePlaintext()
                                    .build();           // channel is the abstraction to connect to a service endpoint
        this.stub =
            TupleSpacesGrpc.newBlockingStub(channel);   // stub is the client side representation of the service
                                                        // we use a blocking stub, which waits synchronously for the response
    }

    /**
     * sends a PUT request to the server
     * @param tuple the tuple to put in the tuple space
     */
    public void requestPut(String tuple, int[] delay) {
        TupleSpacesOuterClass.PutRequest request =
            TupleSpacesOuterClass.PutRequest
                                .newBuilder()
                                .setNewTuple(tuple)
                                .build();               // construct a new Protobuffer object to send as request to the server

        if (this.DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " sending PUT request... tuple:" + tuple);
        }

        try {
            TupleSpacesOuterClass.PutResponse response;

            if (delay != null) {
                String delayMetadata = delaysInString(delay);

                Metadata metadata = new Metadata();
                metadata.put(CUSTOM_HEADER_KEY, delayMetadata);

                response = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).put(request); // send the request with metadata to the server and get the response from it
            }
            else {
                response = stub.put(request);                                                                       // send the request to the server and get the response from it
            }

            String result = response.getOk();
            System.out.println(result + "\n");
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " PUT request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
        }
    }

    /**
     * sends a READ request to the server
     * @param pattern the pattern to search for in the tuple space
     * @return the tuple that matches the pattern
     */
    public String requestRead(String pattern, int[] delay) {
        TupleSpacesOuterClass.ReadRequest request =
            TupleSpacesOuterClass.ReadRequest.newBuilder()
                                .setSearchPattern(pattern)
                                .build();               // construct a new Protobuffer object to send as request to the server

        if (this.DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " sending READ request... pattern: " + pattern);
        }

        try {
            TupleSpacesOuterClass.ReadResponse response;

            if (delay != null) {
                String delayMetadata = delaysInString(delay);

                Metadata metadata = new Metadata();
                metadata.put(CUSTOM_HEADER_KEY, delayMetadata);

                response = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).read(request); // send the request with metadata to the server and get the response from it
            }
            else {
                response = stub.read(request);                                                                       // send the request to the server and get the response from it
            }

            System.out.println("OK");
            return response.getResult();
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " READ request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * sends a TAKE request to the server
     * @param pattern the pattern to search for in the tuple space
     * @return the tuple that matches the pattern
     */
    public String requestTake(String pattern, int[] delay) {
        TupleSpacesOuterClass.TakeRequest request = 
            TupleSpacesOuterClass.TakeRequest
                                .newBuilder()
                                .setClientId(client_id)
                                .setSearchPattern(pattern)
                                .build();               // construct a new Protobuffer object to send as request to the server

        if (this.DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " sending TAKE request... pattern: " + pattern);
        }

        try {
            TupleSpacesOuterClass.TakeResponse response;

            if (delay != null) {
                String delayMetadata = delaysInString(delay);

                Metadata metadata = new Metadata();
                metadata.put(CUSTOM_HEADER_KEY, delayMetadata);

                response = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).take(request); // send the request with metadata to the server and get the response from it
            }
            else {
                response = stub.take(request);                                                                       // send the request to the server and get the response from it
            }

            System.out.println("OK");
            return response.getResult();
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " TAKE request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * sends a GET-TUPLE-SPACES-STATE request to the server
     * @return the state of the tuple spaces
     */
    public List<String> requestGetTupleSpacesState() {
        TupleSpacesOuterClass.getTupleSpacesStateRequest request =
            TupleSpacesOuterClass.getTupleSpacesStateRequest
                                .newBuilder()
                                .build();   // construct a new Protobuffer object to send as request to the server
        
        if (this.DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " sending GET-TUPLE-SPACES-STATE request...");
        }

        try {
            TupleSpacesOuterClass.getTupleSpacesStateResponse response = stub.getTupleSpacesState(request); // send the request to the server and get the response from it
            System.out.println("OK");
            
            int i = 0;
            int tupleSpacesStateCount = response.getTupleCount();
            List<String> tupleSpacesState = new ArrayList<>();

            while (i < tupleSpacesStateCount) {
                tupleSpacesState.add(response.getTuple(i)); 
                i++;
            }

            return tupleSpacesState;
        }
        catch (StatusRuntimeException e) {
            if (this.DEBUG) {
                System.err.println("[\u001B[34mDEBUG\u001B[0m] Client " + client_id + " GET-TUPLE-SPACES-STATE request \u001B[31merror\u001B[0m: " + e.getMessage());
            }
            return null;
        }
    }

    
    public void shutdown() {
        channel.shutdown();
    }


    public String delaysInString(int[] delays) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < delays.length; i++) {
            sb.append(delays[i]);
            if (i < delays.length - 1) { sb.append(" "); }
        }

        return sb.toString();
    }

}


package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.lang.InterruptedException;


public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println(ServerMain.class.getSimpleName());

        // check arguments
        if (args.length != 1 && args.length != 2) {
            System.err.println("Invalid number of arguments");
            System.err.printf("Usage: java %s <port> [-debug]%n", ServerMain.class.getName());
            return;
        }

        // check if debug mode is enabled
        final boolean DEBUG = (args.length == 2 && args[1].equals("-debug"));

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Debug mode enabled");
        
            // receive and print arguments
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] arg[%d] = %s%n", i, args[i]);
            }
        }

        final int port = Integer.parseInt(args[0]);

        // create a new gRPC server instance on the specified port
        Server server = ServerBuilder.forPort(port)
                                    .addService(new TupleSpacesServiceImpl(DEBUG))
                                    .build();

        // start the server
        server.start();
        // server threads are running in the background

        if (DEBUG) {
            System.out.printf("[\u001B[34mDEBUG\u001B[0m] Server started, listening on port: %d\n\n", port);
        }

        // do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }
}


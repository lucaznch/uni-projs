package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.lang.InterruptedException;



/**
 * The front-end is simultaneously a server (as it receives and responds to client requests)
 * and a client (as it makes remote invocations to the TupleSpaces server(s).
 * 
 * when launched, it receives the port on which it should offer its remote service,
 * as well as the hostname and port pairs of the TupleSpaces servers it will interact with
 */
public class FrontendMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println(FrontendMain.class.getSimpleName());

        // check arguments
        if (args.length != 2 && args.length != 3) {
            System.err.println("Invalid number of arguments");
            System.err.printf("Usage: java %s <port> <host:port> [-debug]%n", FrontendMain.class.getName());
            return;
        }

        final boolean DEBUG = (args.length == 3 && args[2].equals("-debug"));

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Debug mode enabled");
        
            // receive and print arguments
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] arg[%d] = %s%n", i, args[i]);
            }
        }

        final int port = Integer.parseInt(args[0]);
        final String target = args[1];

        // create a new gRPC server instance on the specified port for client communication
        Server server = ServerBuilder.forPort(port)
                        .addService(new FrontendImpl(DEBUG, target))
                        .build();

        // start the server
        server.start();
        // server threads are running in the background

        if (DEBUG) {
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Frontend started, listening on port: %d\n\n", port);
        }

        // do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }
}


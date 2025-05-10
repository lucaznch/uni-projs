package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

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
        if (args.length != 4 && args.length != 5) {
            System.err.println("Invalid number of arguments");
            System.err.printf("Usage: java %s <port> <host-server-r1:port-server-r1> <host-server-r2:port-server-r2> <host-server-r3:port-server-r3> [-debug]%n", FrontendMain.class.getName());
            return;
        }

        final boolean DEBUG = (args.length == 5 && args[4].equals("-debug"));

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Debug mode enabled");
        
            // receive and print arguments
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] arg[%d] = %s", i, args[i]);
                if (i == 0 || i == 4) { System.err.println(); }
                else { System.err.printf(" -------> \u001B[33mSERVER\u001B[0m %d%n", i-1); }
            }
        }

        // TODO:    make frontend more robust by allowing for a variable number of servers
        //          tip: look at lab 7 code (multi-step branch) for inspiration
        //          for now, we assume there are exactly 3 servers
        final int numServers = 3;

        final int port = Integer.parseInt(args[0]);

        final String[] servers = new String[numServers];
        servers[0] = args[1];
        servers[1] = args[2];
        servers[2] = args[3];

        // create a new gRPC server instance on the specified port for client communication
        Server server = ServerBuilder.forPort(port)
                        .addService(ServerInterceptors.intercept(new FrontendImpl(DEBUG, numServers, servers), new HeaderServerInterceptor()))
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


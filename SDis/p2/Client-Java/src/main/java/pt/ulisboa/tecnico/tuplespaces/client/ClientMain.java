package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        // check arguments
        if (args.length != 2 && args.length != 3) {
            System.err.println("Invalid number of arguments!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id> [-debug]");
            return;
        }

        // get the host and the port of the server or front-end
        final String host_port = args[0];
        final int client_id = Integer.parseInt(args[1]);

        // check if debug mode is enabled
        final boolean DEBUG = (args.length == 3 && args[2].equals("-debug"));

        if (DEBUG) {
            System.err.println("[\u001B[34mDEBUG\u001B[0m] Debug mode enabled");

            // receive and print arguments
            System.err.printf("[\u001B[34mDEBUG\u001B[0m] Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.printf("[\u001B[34mDEBUG\u001B[0m] arg[%d] = %s%n", i, args[i]);
            }
        }

        // create the parser and start the command line interface
        CommandProcessor parser = new CommandProcessor(new ClientService(host_port, client_id, DEBUG));
        parser.parseInput();

    }
}

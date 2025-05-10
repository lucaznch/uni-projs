import sys
from typing import List
from client_service import ClientService
from command_processor import CommandProcessor

class ClientMain:
    @staticmethod
    def main(args: List[str]):
        print("Python Client")

        if len(args) != 2 and len(args) != 3:
            print("Argument(s) missing!", file=sys.stderr)
            print("Usage: python3 client_main.py <host:port> <client_id> [-debug]", file=sys.stderr)
            return

        DEBUG = len(args) == 3 and args[2] == "-debug"

        if DEBUG:
            print("[\u001B[34mDEBUG\u001B[0m] Debug mode enabled")

            # receive and print arguments
            print(f"[\u001B[34mDEBUG\u001B[0m] Received {len(args)} arguments", file=sys.stderr)
            for i, arg in enumerate(args):
                print(f"[\u001B[34mDEBUG\u001B[0m] arg[{i}] = {arg}", file=sys.stderr)

        # Get the host and port of the server or front-end
        host_port = args[0]
        client_id = args[1]

        parser = CommandProcessor(ClientService(host_port, client_id, DEBUG))
        parser.parse_input()


if __name__ == "__main__":
    ClientMain.main(sys.argv[1:])
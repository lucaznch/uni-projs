from typing import List
import time

class CommandProcessor:
    SPACE = " "
    BGN_TUPLE = "<"
    END_TUPLE = ">"
    PUT = "put"
    READ = "read"
    TAKE = "take"
    SLEEP = "sleep"
    EXIT = "exit"
    GET_TUPLE_SPACES_STATE = "getTupleSpacesState"

    def __init__(self, client_service):
        self.client_service = client_service

    def parse_input(self):
        exit_flag = False
        while not exit_flag:
            try:
                line = input("> ").strip()
                split = line.split(self.SPACE)
                command = split[0]

                if command == self.PUT:
                    self.put(split)
                elif command == self.READ:
                    self.read(split)
                elif command == self.TAKE:
                    self.take(split)
                elif command == self.SLEEP:
                    self.asleep(split)
                elif command == self.GET_TUPLE_SPACES_STATE:
                    self.get_tuple_spaces_state()
                elif command == self.EXIT:
                    exit_flag = True
                else:
                    self.print_usage()
            except EOFError:
                break
        self.client_service.shutdown()

    def put(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        # get the tuple
        tuple_data = split[1]

        # make the call to the server
        self.client_service.request_put(tuple_data)

    def read(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        # get the tuple
        tuple_data = split[1]

        # make the call to the server and get the response
        print(self.client_service.request_read(tuple_data) + "\n")

    def take(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        # get the tuple
        tuple_data = split[1]

        # make the call to the server and get the response
        print(self.client_service.request_take(tuple_data) + "\n")

    def asleep(self, split: List[str]):
        if len(split)!=2:
            self.print_usage()
            return
        
        #checks if string can be parsed as integer
        try:
            time_value = int(split[1])
        except ValueError as e:
            self.print_usage()
            return
        
        try:
            time.sleep(time_value)
        except KeyboardInterrupt as e:
            raise RuntimeError

    def get_tuple_spaces_state(self):
        # make the call to the server and get the response
        response = self.client_service.request_get_tuple_spaces_state()

        if not response:
            print("[]\n")
        else:
            formatted_response = "[" + ", ".join(response) + "]"
            print(formatted_response + "\n")

    def print_usage(self):
        print("Usage:\n"
              "- put <element[,more_elements]> [<delayServer1(seconds)> <...> <delayServerN(seconds)>]\n"
              "- read <element[,more_elements]> [<delayServer1(seconds)> <...> <delayServerN(seconds)>]\n"
              "- take <element[,more_elements]> [<delayServer1(seconds)> <...> <delayServerN(seconds)>]\n"
              "- getTupleSpacesState\n"
              "- sleep <delay (seconds)>\n"
              "- exit\n")

    def input_is_valid(self, input_data: List[str]) -> bool:
        if (len(input_data) < 2
                or not input_data[1].startswith(self.BGN_TUPLE)
                or not input_data[1].endswith(self.END_TUPLE)
                or len(input_data) > 2):
            return False
        return True
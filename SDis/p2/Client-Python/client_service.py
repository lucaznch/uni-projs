import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

import grpc
import TupleSpaces_pb2 as pb2
import TupleSpaces_pb2_grpc as pb2_grpc


class ClientService:
    def __init__(self, host_port: str, client_id: int, debug: bool):
        self.DEBUG = debug
        self.host_port = host_port
        self.client_id = client_id
        self.channel = grpc.insecure_channel(self.host_port)    # create a channel
        self.stub = pb2_grpc.TupleSpacesStub(self.channel)      # create a stub
    
    def request_put(self, tuple_data: str):
        # construct a new Protobuffer object to send as request to the server
        request = pb2.PutRequest(newTuple=tuple_data)

        if self.DEBUG:
            print(f"[\u001B[34mDEBUG\u001B[0m] Client {self.client_id} sending PUT request... tuple: {tuple_data}", file=sys.stderr)

        try:
            # send the request to the server and get the response back
            response = self.stub.put(request)
            print("OK\n")
        except grpc.RpcError as e:
            if self.DEBUG:
                print(f"[\u001B[31mDEBUG\u001B[0m] Client {self.client_id} PUT request \u001B[31merror\u001B[0m: {e.details}", file=sys.stderr)

    def request_read(self, pattern: str) -> str:
        # construct a new Protobuffer object to send as request to the server
        request = pb2.ReadRequest(searchPattern=pattern)

        if self.DEBUG:
            print(f"[\u001B[34mDEBUG\u001B[0m] Client {self.client_id} sending READ request... pattern: {pattern}", file=sys.stderr)

        try:
            # send the request to the server and get the response back
            response = self.stub.read(request)
            print("OK")
            return response.result
        except grpc.RpcError as e:
            if self.DEBUG:
                print(f"[\u001B[31mDEBUG\u001B[0m] Client {self.client_id} READ request \u001B[31merror\u001B[0m: {e.details}", file=sys.stderr)
            return None

    def request_take(self, pattern: str) -> str:
        # construct a new Protobuffer object to send as request to the server
        request = pb2.TakeRequest(clientId=int(self.client_id), searchPattern=pattern)

        if self.DEBUG:
            print(f"[\u001B[34mDEBUG\u001B[0m] Client {self.client_id} sending TAKE request... pattern: {pattern}", file=sys.stderr)

        try:
            # send the request to the server and get the response back
            response = self.stub.take(request)
            print("OK")
            return response.result
        except grpc.RpcError as e:
            if self.DEBUG:
                print(f"[\u001B[31mDEBUG\u001B[0m] Client {self.client_id} TAKE request \u001B[31merror\u001B[0m: {e.details}", file=sys.stderr)
            return None    

    def request_get_tuple_spaces_state(self):
        # construct a new Protobuffer object to send as request to the server
        request = pb2.getTupleSpacesStateRequest()

        if self.DEBUG:
            print(f"[\u001B[34mDEBUG\u001B[0m] Client {self.client_id} sending GET_TUPLE_SPACES_STATE request...", file=sys.stderr)

        try:
            # send the request to the server and get the response back
            response = self.stub.getTupleSpacesState(request)
            print("OK")
            return response.tuple
        except grpc.RpcError as e:
            if self.DEBUG:
                print(f"[\u001B[31mDEBUG\u001B[0m] Client {self.client_id} GET_TUPLE_SPACES_STATE request \u001B[31merror\u001B[0m: {e.details}", file=sys.stderr)
            return None

    def shutdown(self):
        self.channel.close()
        
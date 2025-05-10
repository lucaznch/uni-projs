# TupleSpaces

Distributed Systems Project 2025

- **Group T18**
- **Difficulty level: I am Death incarnate!**


## Getting Started

The overall system is made up of several modules. The definition of messages and services is in _Contract_.

See the [Project Statement](../README.md) for a complete domain and system description.


### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```


### Installation

To compile and install all modules:

```s
mvn clean install
```


### Execution

In the **root** directory of the project, create a virtual environment, activate it and install the required packages:
```bash
python3 -m venv .venv
source .venv/bin/activate
python3 -m pip install grpcio grpcio-tools
```

In the `Contract` directory, execute the following command:
```bash
mvn install
mvn exec:exec
```

In the `ReplicaServer` directory, for each server:
- Run the server with specific arguments:
    ```bash
    mvn compile exec:java -Dexec.args="<port> [-debug]"
    ```
    - e.g., `mvn compile exec:java -Dexec.args="3001 -debug"`
- Run the server with predefined arguments from **pom.xml**:
    ```bash
    mvn compile exec:java
    ```

In the `Frontend` directory:
- Run the frontend with specific arguments:
    ```bash
    mvn compile exec:java -Dexec.args="<frontendPort> <server1-host:server1-port> <server2-host:server2-port> <server3-host:server3-port> [-debug]"
    ```
    - e.g., `mvn compile exec:java -Dexec.args="2001 localhost:3001 localhost:3002 localhost:3003 -debug"`
- Run the frontend with predefined arguments from **pom.xml**:
    ```bash
    mvn compile exec:java
    ```

In the `Client-Java` directory:
- Run the client with specific arguments:
    ```bash
    mvn compile exec:java -Dexec.args="<host:port> <client_id> [-debug]"
    ```
    - e.g., `mvn compile exec:java -Dexec.args="localhost:2001 1"`
- Run the client with predefined arguments from **pom.xml**:
    ```bash
    mvn compile exec:java
    ```

In the `Client-Python` directory, run the client with specific arguments:
```bash
python3 client_main.py <host:port> <client_id> [-debug]
```
- e.g., `python3 client_main.py localhost:2001 2`


>[!NOTE]
> client IDs should be unique!


## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.


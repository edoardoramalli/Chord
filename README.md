# Chord

Chord is a protocol and algorithm for a distributed peer-to-peer hash table. The key to value pairs of different computers (known as "nodes"); to the node will store the values for all the keys for which it is responsible. Chord specifies how to identify nodes, and how to find value for a given key to the node responsible for that key.
The provided implementation has strictly following logical behaviour as described in the original [paper].

### New Features
Here is an implementation of Chord in JAVA enriched by:
- Controller: It is used to monitor the statistical parameters of the network, but it has not a fundamental rule in the operation of the system.
- SocketManager: It is able to introduce relevant optimization by managing the sockets connection between the nodes in the network.

### Usage
In order to launch a node instance of Chord is necessary to pass to executable file some parameters, specified by following flags.
The general signature is:
```sh
$ java -jar main.jar -t type [-d dim] [-cip ipc] [-cp portc] [-jip ipj] [-jp portj] [-deb]

```
- `type`: specify the tipology of the node. If this flag is set to '0' (zero), an instance of controller is created. Instead, if it set to 1, an instance of a Chord node is created. In this case the node is also responsable of the creation of the Chord network, for this reason the dimension flag is not optional. Finally, if type is equal to 2, means that the node is a normal one that join the Chord network on the ip and the port specify by the parameter, that in this case are not optionale, 'ipj' and 'portj'. In the last two cases in also necessary to specify the network parameters to contact the controller node.
- `dim`: specify the power of 2 that its result represent the dimension of the Chord network.
- `cip`: it is the ip address of the controller.
- `cp`: it is the port of the controller that is listening for incoming connection.
- `jip`: it is the ip address of a node on which the current node practise the join method.
- `jp`: it is the port number of the node on which the join method is performed.
- `deb`: specify if you want to launch the node in the debug mode. In this way, throught the console you can interact with the node.

### Tests and Results
To collect some quantitive results from the Chord network, a controller is designed. The subjects under observation is the time required by the network to stabilize itself, accept a new pair key-value, retrive a key from the value and lookup for a node. So every time one of this action is performed a message is sent to the controller to keep track the starting and the ending time of the operation.
We assume that the Chord network is stable when between two succcesive instant all the nodes in the network don't change their status (Finger table and successor list).
The test is performed with a number of increasing nodes present in the net. We have to precise that the way in which this tests are performed have not indifferent inpact on the result, because matter the number of nodes involved in each operation.
![alt text](https://github.com/edoardoramalli/Chord/blob/master/img/plot1.png)

[paper]: <https://pdos.csail.mit.edu/papers/ton:chord/paper-ton.pdf>



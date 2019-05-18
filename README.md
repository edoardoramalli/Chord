# Chord

Chord is a protocol and algorithm for a distributed peer-to-peer hash table. The key to value pairs of different computers (known as "nodes"); to the node will store the values for all the keys for which it is responsible. Chord specifies how to identify nodes, and how to find value for a given key to the node responsible for that key.
The provided implementation has strictly following logical behaviour as described in the original [paper].

### New Features
Here is an implementation of Chord in JAVA enriched by:
- Controller: It is used to monitor the statistical parameters of the network, but it has not a fundamental rule in the operation of the system.
- SocketManager: It is able to introduce relevant optimization by managing the sockets connection between the nodes in the network.

### Utilization

```sh
$ cd dillinger
$ npm install -d
$ node app
```


```sh
$ npm install --production
$ NODE_ENV=production node app
```

### Tests and Results

```sh
$ karma test
```
#### Building for source
For production release:
```sh
$ gulp build --prod
```

```sh
$ gulp build dist --prod
```

```sh
cd dillinger
docker build -t joemccann/dillinger:${package.json.version} .
```
 `${package.json.version}`

```sh
docker run -d -p 8000:8080 --restart="always" <youruser>/dillinger:${package.json.version}
```

```sh
127.0.0.1:8000
```

[paper]: <https://pdos.csail.mit.edu/papers/ton:chord/paper-ton.pdf>



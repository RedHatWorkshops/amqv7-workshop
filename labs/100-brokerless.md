# Brokerless routing

Although the Interconnect Router can be used to build large-scale, geographically distributed broker clusters, it also provides a mechanism to do inter-network RPC against a transparent backplane where producers and consumers are decoupled in location (just like with a broker, but without a broker). This gives some of the best of both the messaging and RPC worlds. Let's take a look.


In this lab we'll take a look at configuring the Interconnect Router to route messages to and from an AMQ7 broker. Clients can talk directly to the router and think they're talking to a broker.

Take a look in the `labs/qdr/brokerless` folder. We have three different Interconnect configuration files and a couple different scripts to control the demo. We'll be using docker to run this demo, so verify you have access to a docker host (either on your laptop with Docker for Mac/Windows, or using Minikube, or using Red Hat CDK 3.0). 


Let's take a look at the salient parts of the configuration:

router3.conf:

```json
listener {
  role: normal
  host: 0.0.0.0
  port: 5672
  linkCapacity: 1000
}

... 

listener {
  role: inter-router
  host: 0.0.0.0
  port: 10003
}

```

Here we see that we're opening a listener for port `5672` and for `10003`. The `5672` port is for AMQP clients to connect directly to the router mesh. Port `10003` is for other routers to connect. Note the `role` each listener has (ie, `normal` and `inter-router`). We would expect the other routers to connect to this router via is listener on `10003`. Also, this router doesn't have any connectors to any other routers.

router2.conf:

```json
listener {
  role: inter-router
  host: 0.0.0.0
  port: 10002
}

...

connector {
    role: inter-router
    host: router3
    port: 10003
    saslMechanisms: ANONYMOUS
}

```

In this configuration file, we see that there are *no* AMQP listeners (nothing listening on `5672`, and no listeners labeled `normal`). There is a single listener on port `10002`. We do see a single connector pointing to the router3 `10003` port. Let's look at the last router:

router1.conf:

```json

listener {
  role: normal
  host: 0.0.0.0
  port: 5672
  linkCapacity: 1000
}


...

connector {
    role: inter-router
    host: router2
    port: 10002
    saslMechanisms: ANONYMOUS
}

connector {
    role: inter-router
    host: router3
    port: 10003
    saslMechanisms: ANONYMOUS
}

```

In this configuration, we see that this router also exposes an AMQP port on `5672`. It also connects to the other routers, router 2 on port `10002` and router 3 on `10003` 

Now that we see how the routers are connected with each other, let's bootstrap the router mesh. Make sure you're in the `labs/qdr/brokerless` folder and type:

```bash
./start.sh
```

This will bootstrap each broker with their respective configuration files and link them together properly. The containers will be running in the background, so you can take a look at each ones logs to verify they're up correctly:

```bash
docker logs router1
4area\xa1\x010"
Thu May 25 13:42:50 2017 ROUTER_CORE (trace) Core action 'delete_delivery'
Thu May 25 13:42:50 2017 ROUTER_CORE (trace) Core action 'delete_delivery'
Thu May 25 13:42:50 2017 ROUTER_CORE (trace) Core action 'link_flow'
Thu May 25 13:42:50 2017 ROUTER_CORE (trace) Core action 'link_flow'
Thu May 25 13:42:50 2017 SERVER (trace) [2]:1 <- @flow(19) [next-incoming-id=2600, incoming-window=2147483647, next-outgoing-id=0, outgoing-window=2147483647, handle=0, delivery-count=2600, link-credit=250, drain=false]
Thu May 25 13:42:50 2017 SERVER (trace) [2]:0 <- @transfer(20) [handle=0, delivery-id=2604, delivery-tag=b"X\x14\x00\x00\x00\x00\x00\x00", message-format=0, settled=true, more=false] (229) "\x00Sr\xd1\x00\x00\x00E\x00\x00\x00\x04\xa3\x0ex-opt-qd.trace\xd0\x00\x00\x00\x0f\x00\x00\x00\x01\xa1\x090/Router3\xa3\x10x-opt-qd.ingress\xa1\x090/Router3\x00Ss\xd0\x00\x00\x00\x1f\x00\x00\x00\x06@@\xa1\x14amqp:/_local/qdhello@@@\x00St\xd1\x00\x00\x00\x13\x00\x00\x00\x02\xa1\x06opcode\xa1\x05HELLO\x00Sw\xd1\x00\x00\x00N\x00\x00\x00\x08\xa1\x04seen\xd0\x00\x00\x00\x16\x00\x00\x00\x02\xa1\x07Router2\xa1\x07Router1\xa1\x08instance\x81\x00\x00\x00\x00Y&\xd5\xd9\xa1\x02id\xa1\x07Router3\xa1\x04area\xa1\x010"

```

To run the demo, make sure you start a receiver first:

```bash
./receiver_router3.sh
```

This will connect to router 3 on its AMQP port (`5672`) and begin listening for messages on address `/myAddress`.

Let's start the sender:

```bash
./sender_router1.sh
```

This will connect to the router 1 on its AMQP port and send messages to address `/myAddress`. You should see indications of a single message flowing through the mesh (via the sender/receiver logs).

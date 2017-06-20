## Networks of Brokers in AMQ7 Broker (Clustering)   

This worksheet covers clustering AMQ7 Brokers. 
By the end of this you should know:

1. Clustering concepts of AMQ7
   * Discovery
   * Cluster bridges
   * Routing of messages
   * Client side load balancing
   * Server side load balancing
   
2. How to configure Clustering
   * Configuring Discovery
   * Configuring A cluster
   * Configuring Load Balancing 


### AMQ7 Clustering Concepts

multiple instances of AMQ7 Brokers can be grouped together to share message processing load.
Each Broker manages its own messages and connections and is connected to other brokers with
Cluster bridges that are used to send Topology information, such as queues and consumers, 
as well as load balancing messages.
 
### Simple 2 node cluster

Lets create 2 clustered brokers using the CLI, firstly thos since AMQ 7 uses UDP for discovery 
you will need to ensure that a loopback address is created, this will allow UDP to work on the same machine.
On Linux run the command

      route add -net 224.0.0.0 netmask 240.0.0.0 dev lo
      
On a Mac this will be something like

      ? find this out
      
Now lets create a cluster of 2 brokers by running the CLI commands 

      (A_MQ_Install_Dir)/bin/artemis create  --user admin --password password --role admin --allow-anonymous y --clustered --host 127.0.0.1 --cluster-user clusterUser --cluster-password clusterPassword  --max-hops 1 broker1

and

      (A_MQ_Install_Dir)/bin/artemis create  --user admin --password password --role admin --allow-anonymous y --clustered --host 127.0.0.1 --cluster-user clusterUser --cluster-password clusterPassword  --max-hops 1 --port-offset 100 broker2

Now start both brokers using the run command, for instance

      "(Broker1_Home)/broker1/bin/artemis" run
      
What you should see is each Broker discovering each other and connection a cluster bridge, 
you should see a log message on each Broker showing this, something like:

```bash
   12:42:35,488 INFO  [org.apache.activemq.artemis.core.server] AMQ221027: Bridge ClusterConnectionBridge@1d90c678 [name=$.artemis.internal.sf.my-cluster.8d25b0ff-55ad-11e7-bfb2-e8b1fc559583, queue=QueueImpl[name=$.artemis.internal.sf.my-cluster.8d25b0ff-55ad-11e7-bfb2-e8b1fc559583, postOffice=PostOfficeImpl [server=ActiveMQServerImpl::serverUUID=86fab59a-55ad-11e7-ae52-e8b1fc559583], temp=false]@60ff2ab7 targetConnector=ServerLocatorImpl (identity=(Cluster-connection-bridge::ClusterConnectionBridge@1d90c678 [name=$.artemis.internal.sf.my-cluster.8d25b0ff-55ad-11e7-bfb2-e8b1fc559583, queue=QueueImpl[name=$.artemis.internal.sf.my-cluster.8d25b0ff-55ad-11e7-bfb2-e8b1fc559583, postOffice=PostOfficeImpl [server=ActiveMQServerImpl::serverUUID=86fab59a-55ad-11e7-ae52-e8b1fc559583], temp=false]@60ff2ab7 targetConnector=ServerLocatorImpl [initialConnectors=[TransportConfiguration(name=artemis, factory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory) ?port=61716&host=127-0-0-1], discoveryGroupConfiguration=null]]::ClusterConnectionImpl@943454742[nodeUUID=86fab59a-55ad-11e7-ae52-e8b1fc559583, connector=TransportConfiguration(name=artemis, factory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory) ?port=61616&host=127-0-0-1, address=, server=ActiveMQServerImpl::serverUUID=86fab59a-55ad-11e7-ae52-e8b1fc559583])) [initialConnectors=[TransportConfiguration(name=artemis, factory=org-apache-activemq-artemis-core-remoting-impl-netty-NettyConnectorFactory) ?port=61716&host=127-0-0-1], discoveryGroupConfiguration=null]] is connected
```

You can also log into the HawtIO Console by going to 'http://localhost:8161/hawtio' and click on the 
'ARTEMIS' tab then the 'Diagram' tab. You will see 2 Brokers and also the Store and Forward Addresses
and queues:

![alt text](etc/diagram.png "Broker Diagram")


We now have a cluster of 2 brokers, lets look in more detail at the configuration.

#### Discovery

When a Clustered Broker is started the first thing it does is try to discover another Broker in the cluster.
It will keep doing this until it finds a Broker at which time it will try to create a Cluster Bridge to it.
By default the Broker will use UDP to broadcast its location and to also discover other Brokers.


The first thing to notice in the configuration file is the connector config that will be broadcast to other Brokers, this 
looks like:

```xml
<connector name="artemis">tcp://127.0.0.1:61616</connector>
```

A Broadcast Group then defines how a Broker broadcasts the connector info, this looks something like:

```xml
<broadcast-group name="bg-group1">
   <group-address>231.7.7.7</group-address>
   <group-port>9876</group-port>
   <broadcast-period>5000</broadcast-period>
   <connector-ref>artemis</connector-ref>
</broadcast-group>

```

This configuration will broadcast the 'artemis' connector info over the UDP address 231.7.7.7:9876 every 5 secosnds.

Now a Broker needs to be able discover the above broadcast, this is done via a discovery group. This looks like:

```xml
<discovery-group name="dg-group1">
   <group-address>231.7.7.7</group-address>
   <group-port>9876</group-port>
   <refresh-timeout>10000</refresh-timeout>
</discovery-group>
```

This configuration doesn't do anything by itself but is referenced by a cluster connection configuration, which looks like:

```xml
<cluster-connection name="my-cluster">
   <connector-ref>artemis</connector-ref>
   <message-load-balancing>ON_DEMAND</message-load-balancing>
   <max-hops>1</max-hops>
   <discovery-group-ref discovery-group-name="dg-group1"/>
</cluster-connection>
```

You can see that the 'discovery-group-ref' references a discovery group, once started the Broker will listen on 
the UDP address 231.7.7.7:9876 for other Brokers broadcasting.

Once the Broker has discovered a target broker it will try to create a Cluster Bridge to that Broker, we refer to this as initial discovery.
Once initial discovery is complete all other discovery is done over the Cluster Bridge, in a 2 node cluster it would happen 
like so:

   1. The source Broker sends its full Topology over the cluster bridge to its target Broker, This includes a list of Brokers it 
   is aware of , including itself configured by the connector-ref in the cluster-connection config, and a list of queues and consumers.
   2. The target broker then uses the list of Brokers to create its own cluster bridges, in this case back to the source Broker.
   3. The target Broker then sends its own Topology over its cluster bridges.
   4. Both Broker create any queues based on the Topology received.
   

##### Discovery without UDP (Static Connectors), Optional

If UDP is not available then Brokers can be statically configured. This is done purely thru connectors.

lets update broker1 and 2 to use static connectors.

Firstly remove both the broadcast and discovery groups configuration completely.

Then add a connector that points to the other broker, so on broker 1 it would look like:

```xml
<connector name="discovery-connector">tcp://127.0.0.1:61716</connector>
```

broker 2 would look like

```xml
<connector name="discovery-connector">tcp://127.0.0.1:61616</connector>
```

Lastly remove the discovery-group-ref from the cluster-connection config and replace it with the following:

```xml
<static-connectors>
   <connector-ref>discovery-connector</connector-ref>
</static-connectors>
```

Now if you restart the brokers you will again see them form a cluster.

   > Note
   > The static connectors list can contain all the possible brokers in the cluster, 
   > however it only needs 1 to be available to connect to.
   

#### Client Side load balancing

Client Side balancing is the ability to spread connections across multiple brokers, 
currently only the core JMS client supports this. This is done via load balancing policies configured on the 
Connection Factory. If using JNDI the `jndi.properties`would look like:

    java.naming.factory.initial=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
    connection.myConnectionFactory=tcp://localhost:61616?loadBalancingPolicyClassName=org.apache.activemq.artemis.api.core.client.loadbalance.RandomConnectionLoadBalancingPolicy

The available policies are

-   Round Robin.
-   Random. 
-   Random Sticky. 
-   First Element. 

You can also implement your own policy by implementing an interface

#### Message Load Balancing

The Message Load balancing Policy configures how messages are load balanced around the cluster.
This is configured in the cluster-connection, like so.

```xml
<message-load-balancing>ON_DEMAND</message-load-balancing>
```

By default it is ON_DEMAND, which means that messages will be round robined around Brokers that have
available consumers.

   > Note
   > Messages are routed at the point they arrive at the broker and before they arrive on a queue. 
   > They wil leitehr route to a local queue or to a queue on another broker

We can test this using the cli, but firstly each broker needs to be stopped and the clustered queues configured,
add the following queue to each broker:

```xml
<address name="myQueue">
   <anycast>
      <queue name="myQueue"/>
   </anycast>
</address>
```

And then restart the brokers.

firstly lets send 20 messages to broker 1:

```bash
 ${ARTEMIS_HOME}/bin/artemis producer --url tcp://localhost:61616 --message-count 20 --destination queue://myQueue
```

Since there are no consumers these will simple be delivered to the local queue on broker 1. We can test this by trying
to consume from broker 2:

```bash
  ${ARTEMIS_HOME}/bin/artemis consumer --url tcp://localhost:61716 --destination queue://myQueue --message-count 10
```

This should just hang without receiving any message. Now try broker 1

```bash
${ARTEMIS_HOME}/bin/artemis consumer --url tcp://localhost:61616 --destination queue://myQueue --message-count 20
```

The client will receive all 20 message.

Now restart both receiving clients with --message-count 10

They will sit there awaiting messages, now send some another 20 messages. What you see this time is the messages round robined on demand.

Now try the first part of this again, but this time setting the load balancing to strict, like so:

```xml
<message-load-balancing>STRICT</message-load-balancing>
```

This time the messages are load balanced even tho no consumer exist.
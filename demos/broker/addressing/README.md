# Addressing in AMQ 7 broker

This worksheet covers the addressing model in the AMQ 7 broker.  By the end of this worksheet you should know:

1. The core addressing concepts:
    * How routing works
    * How protocol managers implemented various address semantics using the Core API
    * How to define address semantics in broker configuration
2. How to configure:
    * address-settings
    * addresses with wildcards
    * basic publish subscribe and point to point addresses
    * pre-configured subscription queues
    * shared subscription queues (usable across protocols)

## Addressing Concepts
There are 3 main concepts within the AMQ 7 broker which are responsible for a variety of addressing behaviours:

1. **Queue**: Contains a unique name, a routing type and an ordered list of messages (as well as some more advanced settings which we will touch on later).
2. **Address**: Contains a unique name, a list of queues and a set of enabled routing types.
3. **Routing Type**: Either ANYCAST or MULTICAST.  Defines how messages are routed from an address to its queue(s).

Note: There is no such thing as a "topic" in AMQ 7.  There are only addresses, queues and routing types.  We'll show later in this worksheet how publish subscribe works within AMQ 7.

### Routing messages to queues
When the broker receives a message from a publisher (in any protocol) the protocol manager to which the publisher is speaking will interact with the Core API to route the message to zero or more queues.  We can think of this routing as **inbound** routing, from addresses to queues.  The **inbound** routing logic works as follows:

1. Find all the addresses that match (some addresses may have wildcards and therefore there could be more than 1).
2. Route the message to **all** of the associated queues with a "MULTICAST" routing type and to **one** of the "ANYCAST" queues.

Note.  Queue filtering is also done on inbound routing.  Only messages that match the queue's filter will make it into the queue.

Note.  There is only ever one instance of the message stored within the broker.  Actually what is added to the queue is a pointer to the original message (known internally as a message reference).  The broker uses reference counting to know when it is safe to delete a message.

#### Broker Inbound Routing

In this section we'll demonstrate the broker **inbound** routing logic.  We'll create two addresses, one MULTICAST and one ANYCAST, and we'll show how routing behaviour differs between them.

If you dont already have a broker running, create a new instance and start it:

```bash
$ARTEMIS_HOME/bin/artemis create --allow-anonymous --user admin --password password  myBroker
```

Now add the following lines to the `addresses` element:

```xml
<address name="p2p">
   <anycast>
      <queue name="p2pQueue"/>
   </anycast>
</address>
<address name="pubSub">
   <multicast>
      <queue name="sub1Queue"/>
      <queue name="sub2Queue"/>
   </multicast>
</address>
```

AMQ 7 can pick up new addresses and queues from broker.xml at runtime.  Go the the HawtIO console and verify that the 
addresses and queues were successfully created.  You can do this by logging into http://localhost:8161/hawtio, clicking on the
Artemis tab and navigating to the queues in the left hand panel. You can also view the diagram and see a graphical representation of the queues.

Let's send some messages to the address.  To do this, we'll use the qsend and qreceive tools.

```sh
qsend p2p -m "This message should only be routed to a single queue"
qsend pubSub -m "This message should be routed to multiple queues"
```

Go back to the HawtIO console, navigate to the each of the queues we created and browse their messages.  Notice that sub1 
and sub2 have a copy of the message.  Have a play around to see how various configurations behave.  Try:

1. Adding more than one ANYCAST queue to an address.
2. Adding both ANYCAST queues and MULTICAST queues to the same address.
3. Creating two addresses that contain wildcards with ANYCAST and MULTICAST queues, send messages that match both wildcarded addresses.
    * "." = path separator, "*" wildcard
    
    > Note auto creation of queues can effect how this works, for instance if you create an address topic.* and topic.foo
    > and send a message to topic.bar then this address will be autocreated. Turn auto creation off for this exercise
    
example, try sending messages to topic.foo and topic.bar:

```xml
<address name="topic.*">
   <multicast>
      <queue name="sub6"/>
   </multicast>
</address>
<address name="topic.foo">
   <multicast>
      <queue name="sub7"/>
   </multicast>
</address>
<address name="topic.bar">
   <multicast>
      <queue name="sub8"/>
   </multicast>
</address>
```
    
4. Creating queues with filters.
    * Example xml: 

```xml
<address name="myAddress" >
   <anycast>
      <queue name="filteredQueue">
         <filter string="color='red'" />
      </queue>
      <queue name="nonFilteredQueue" />
   </anycast>
</address>
```

#### Outbound Routing
Before we move on, it's useful to take note of one more component of the AMQ7 Core API - the server consumer.

The server consumer is another Core API concept.  The server consumer receives messages from a single queue within the broker. The broker takes care of distributing messages from queues to server consumers.  they are created by protocol managers and receive messages from queues.  When a message is received by a server consumer, it notifies the protocol manager. A couple of things to note:
    * One queue may have more that one server consumer.
    * The AMQ7 broker takes care of distributing messages from queues to server consumers.
    
For the sake of this tutorial we'll call the routing of messages from queues to consumers **outbound** routing.  We'll take a look later at how these internal consumers play a role within the overall AMQ7 broker picture.

## Addressing Wildcard Syntax
AMQ 7 offers a wildcard syntax.  Wildcards can be used in address names and are used to match received messages.  

A wildcard expression contains words delimited by the character '.' (full stop).

The follow characters are enabled by default:

2. The special characters '#' and '*' also have special meaning and can take the place of a word.
3. The character '#' means 'match any sequence of zero or more words'.
4. The character '*' means 'match a single word'.

So the wildcard 'news.europe.#' would match 'news.europe', 'news.europe.sport', 'news.europe.politics', and 'news.europe.politics.regional' but would not match 'news.usa', 'news.usa.sport' nor 'entertainment'.  The wildcard 'news.*' would match 'news.europe', but not 'news.europe.sport'.  The wildcard 'news.*.sport' would match 'news.europe.sport' and also 'news.usa.sport', but not 'news.europe.politics'.

The wildcard syntax is configurable.  To change the characters used for delimeter, multiple words and a single word add the following to the broker.xml under the <core> element.

```xml
<wildcard-addresses>
  <enabled>true</enabled>
  <delimiter>.</delimiter>
  <any-words>#</any-words>
  <single-word>*</single-word>
</wildcard-addresses>
```

Take a little time to play around with the wildcard syntax.  Use the steps outlined in the previous chapter to create some addresses with wildcards and send some messages.  Use HawtIO to browse the queues.

## Protocol managers and the Core API

As mentioned earlier, protocol managers map protocol specific semantics onto the Core API using the three main concepts - addresses, queues, and routing types.  This section will give a high level overview of how various addressing semantics are acheived using the Core API.  This aim of this section is to give you an insight as to what is happening under the covers and also to lay the foundation for later chapters.

Let's look at JMS and the following address semantics:

1. JMS Queues
2. JMS Topics
    * JMS volatile subscription
    * JMS durable subcription
    * JMS shared volatile subscription
    * JMS shared durable subscription

### JMS Queues
To create a JMS Queue add the following snippet of XML to the broker config:

```xml
<address name="orders" >
   <anycast>
      <queue name="orders" />
   </anycast>
</address>
```

Note: There is a current restriction with JMS naming in that any JMS queue must share the same name as it's address.  
In this case the address and queue name are "orders".

Navigate to the addressing demo and run the JMSQueueReceiver example.

```sh
mvn verify -PJMSQueueReceiver
```

Navigate back to the HawtIO console and view the queue.  You will notice that the consumer count on the orders queue is now 1.

#### What's happening under the covers
Pretty basic stuff, but it's good to know what has happened under the covers.

The example application created a JMS queue consumer.  The JMS AMQP (qpid JMS) client takes care of interacting the the AMQ7 broker 
to set up the relevant addresses, queues, routing types and server consumers.  

How does it do this?

1. The JMS AMQP client library sends an AMQP packet (ATTACH) to the broker.  This is essentially the AMQP way of sending request to create a queue subscriber.  This packet has a bunch of information including the link name, durability, source and target address and some additional information (terminus capabilities) that defines the type of endpoint the user has requested (i.e. Queue).  
2. The AMQP protocol manager handles the packet0 and inteprets the ATTACH information, attempting to satisfy the AMQP request by interacting with the AMQ7 broker Core API.  The steps that take to achieve this are as follows:
    1. Protocol manager inspects the ATTACH packet to determine whether a sender or receiver link should be created.
    2. Protocol manager uses the source address on the packet and interacts with the Core API to query for an address that matches.
    3. The broker checks to see if there are any terminus capabilities set on the ATTACH packet (the AMQP way of requesting certain features of the endpoint), in this case, QUEUE is present.  
    4. The AMQP protocol manager interprets QUEUE as a request for an address with ANYCAST enabled.  It verifies that the matching address has the appropriate routing type, it does.  
    5. The protocol manager attempts to locate a queue associated with the address.  It finds the queue "orders" and so creates a new  server consumer bound to the "orders" queue.
    6. The server consumer triggers when a message is available, informing the protocol manager, which in turn forwards the message onto the JMS client.

Note: It's not important to understand all the details of the JMS to AMQP bindings and interaction within the broker.  The key thing to take away here is that each protocol manager performs it's own protocol specific logic to process a request or handle a packet.  It does this by interacting with the Core API.  When dealing with protocol specific versions of consumers, addresses and subscriptions, protocol managers break down the request into the 4 core concepts we described earlier, addresses, queues, routing types and server consumers.

### Auto creation

Note: The broker can automatically create queues and addresses on demand.  Autocreation is enabled by default out of the box.

Run the JMSQueueReceiverAutoCreate example:

```sh
mvn verify -PJMSQueueReceiverAutoCreate
```

Navigate back to the HawtIO console.  You'll see that an address "auto.created" has been created, with the "ANYCAST" routing type enabled and a queue "auto.created".

### Locking down addresses and queues
In certain scenarios is desirable to lock down queues and addresses.  There are two ways to do this, firstly, authentication and authorisation, this is covered later in the security section of this tutorial.  Secondly, the available addresses and queues can be enforced in broker configuration.

Let's reset all data from the broker, and update our configuration to switch off auto-create and auto-delete settings.

```sh
# Stop the broker then run
cd <broker-instance>; rm -rf data/
```

Change the following lines in the `<address-settings name="#">` element of the broker.xml

```xml
<auto-create-queues>false</auto-create-queues>
<auto-create-addresses>false</auto-create-addresses>
<auto-create-jms-queues>false</auto-create-jms-queues>
<auto-create-jms-topics>false</auto-create-jms-topics>
```

Start the broker again.  This will give us a fresh install.

Try running the JMSQueueReceiverAutoCreate example

```sh
mvn verify -PJMSQueueReceiverAutoCreate
```

Try adding the "auto.create" address with "MULTICAST" enabled and a multicast queue and run the JMSQueueReceiverAutoCreate example again.  What happens?

```xml
<address name="auto.created">
   <multicast>
      <queue name="auto.created" />
   </multicast>
</address>
```

```sh
mvn verify -PJMSQueueReceiverAutoCreate
```

### JMS Topics
To create the equivilent of a JMS topic in the AMQ 7 broker, we need to create an address with the MULTICAST routing type enabled.  To do this add the following XML snippet to the broker.xml

```xml
<address name="events" >
   <multicast/>
</address>
```

#### Subscriptions
As mentioned earlier, there is no such thing as a "topic" in the AMQ 7 broker.  There are only the 4 key concepts we've spoken about in this tutorial.  Subscriptions are implemented by the way of "subscription queues".  Subscription queues have the MULTICAST routing type, which tells the broker that this queue requires a copy of every message that matches the queues address and a bunch of attributes that define some additional semantics.

There are 4 types of subscription defined in the JMS specification.

1. Volatile Subscriptions: Last as long as the client is connected or calls unsubscribe.
2. Durable Subscriptions: Continues whilst the client is offline
3. Volatile Shared Subscriptions: Allows multiple connections to share the subscription.  Lasts as long as a client is connected
4. Durable Shared Subscriptions: Allows multiple connections to share the subscription.  Continues whilst no clients are connected.

When ever one of these subscriptions is created in our JMS AMQP client examples, the JMS AMQP client sends some additional information to the broker, which in turns is intpretted by the AMQP protocol manager.  The AMQP protocol manager then does the relevant address checks and takes care of creating the subscription queues.

We'll take a look at the various subscription queues created by the client.  Open up broker.xml and add the following XML snippet:

```xml
<address name="durableSubscription" >
   <multicast/>
</address>
<address name="volatileSubscription" >
   <multicast/>
</address>
<address name="sharedVolatileSubscription" >
   <multicast/>
</address>
<address name="sharedDurableSubscription" >
   <multicast/>
</address>
```

Now run the JMSSubscriptions example.

```
mvn verify -PJMSSubscriptions
```

Go to the HawtIO console, for each subscription type a corresponding subscription queue will be created.  Take a look at the various attributes of the queues.  This is what distinguishes the various subscription queues from each other.

The main things to note are:

1. The queue name: The protocol manager encodes some information in the queue name.
    * e.g. The durable subscription queue name includes the client's ID.  This is so the protocol manager can locate the queue should the client reconnect at a later date.
2. The Durable field.  This means that the queue will survive a broker restart.
3. The Temporary field.  Temporary queues are deleted once all consumers are disconnected
4. Max Consumers.  Limits the number of consumers at any one time.  -1 means unlimited.

This concludes the section on Protocol managers and the Core API.  In summary.  There are 4 main concepts that lay the foundation for all addressing semantics in the AMQ 7 broker.  Addresses, queues, routing types, and server consumers.  Protocol managers control these objects in order to provide protocol specific semantics to clients.  ANYCAST addresses are akin to JMS queues (or point to point) MULTICAST addresses are akin to JMS topics.

Take some time to play around with the various address types and examples.  What happens if for example multiple queues are added to an ANYCAST address?


## Defining Address Semantics in Broker Configuration
We seen in the previous chapter how protocol managers implement various address semantics particularly around various subscription behaviours.  We used JMS as our example client.  JMS is very client driven, i.e. the client instructs the broker exactly what it requires.  We'll see in this chapter how we can define various address semantics on the broker and use a raw AMQP client.  In this example, the AMQP client has no knowledge of the address semantics provided by the broker it simply registers an interest in an address and starts receiving messages.

You can probably guess how this is going to work.  We define subscription queues up front in broker configuration with various attributes.  The following queue attributes are of interest:

1. max-consumers: Limits the number of consumers for a particular queue (useful for controlling shared subs)
2. purge-on-no-consumers: Removes all messages from a queue when all consumers disconnect.  Useful for defining volatile subscription behaviour.  Note that this is a little different to how protocol managers implement volatile subscriptions.  Protocol managers will usually just delete a queue when it has no consumers.  As the queue itself defines the behaviour, we don't want to delete it, but we will drop all messages and prevent any new ones arriving when no consumers are attached.  Essentially giving us volatile subscription behaviour.

We'll take a look at how to do define the various subscription semantics in XML.  Firstly, go ahead and clear out the broker and reset the broker.xml and restart the broker.

Add the following XML snippet to the broker.xml

```xml
<address name="durableSubscription" >
   <multicast>
      <queue name="dSub1" max-consumers="1" purge-on-no-consumers="false">
         <durable>true</durable>
      </queue>
   </multicast>
</address>

<address name="volatileSubscription" >
   <multicast>
      <queue name="vSub1" max-consumers="1" purge-on-no-consumers="true">
         <durable>false</durable>
      </queue>
   </multicast>
</address>

<address name="sharedVolatileSubscription" >
   <multicast>
      <queue name="svSub1" max-consumers="-1" purge-on-no-consumers="true">
         <durable>false</durable>
      </queue>
   </multicast>
</address>

<address name="sharedVolatileSubscription" >
   <multicast>
      <queue name="sdSub1" max-consumers="-1" purge-on-no-consumers="false">
         <durable>true</durable>
      </queue>
   </multicast>
</address>
```

#### Fully Qualified Queue Names
We explained earlier that clients deal with addresses.  The string the client provides when it publishes or subscribes matches eventually matches an AMQ 7 address name.  The protocol manager is responsible for doing wiring up between the client subscription, then address, the server consumer and internal queue.  However, when doing broker side configuration we actually want to bypass the protocol manager logic for doing this and connect a client direct to a queue.  To do this, we use Fully Qualified Queue Names (FQQN).  The FQQN format is as follows: <address-name>::<queue-name>.

We'll go back to using the AMQP test client used earlier in this chapter and subscribe to one of our pre-defined subscription queues.

```sh
# //<host>:<port>/<address>
qreceive //localhost:5672/durableSubscription::dSub1
```

Try sending some messages to the durableSubscription address

```sh
qsend //localhost:5672/durableSubscription -m "Test Message"
```

Take some time to play around with the various subscription queues and note their behaviour.  As an additional task try pre-creating an address with multiple ANYCAST queues.  Connect receivers and send some messages.

FQQN can be used for a number of use cases, from allowing the pre-creation of shared subscriptions (which can be shared across protocols), to allowing finer control over the life cycle of subscription queues.

### Prefixing

The AMQ 7 broker supports address prefixing, which allows clients to tell the broker the particular routing type that is required for an address (outside of client or protocol specific methods).  Prefixing is enabled on a per protocol basis.  To enable prefixing for the AMQP protocol modify the following lines in the broker.xml

from
```xml 
<acceptor name="amqp">tcp://0.0.0.0:5672?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=AMQP;useEpoll=true;amqpCredits=1000;amqpMinCredits=300</acceptor>
```
to
```xml
<acceptor name="amqp">tcp://0.0.0.0:5672?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=AMQP;useEpoll=true;amqpCredits=1000;amqpMinCredits=300;multicastPrefix=topic://;anycastPrefix=queue://</acceptor>
```

Try receiving from a prefixed address.

```sh
queue://prefixedQueue
```

You'll notice that a new ANYCAST address was created with the name "prefixedQueue" and a single queue "prefixedQueue".  

Note: In the currenct version FQQN and prefixing can not be used together.

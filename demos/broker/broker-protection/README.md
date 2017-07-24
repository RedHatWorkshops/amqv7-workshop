# Protecting the Broker in AMQ 7

This worksheet covers some broker protection features in the AMQ 7 broker.  By the end of this worksheet you should know:

1. How to configure the behavior of the broker when an address becomes "full"
2. How to use resource-limit-settings to restrict how many queues and/or connections a specific user can create
3. How to configure an acceptor to limit the number of allowed TCP connections

## Address-full policies

As messages accumulate in the broker there is a chance that the JVM could run out of heap space.  There are several options to deal with this possibility which can be configured in a matching `<address-setting>`:

* **max-size-bytes** How large an address can be before it's considered "full". The size of an address is calculated by summing the sizes of all the queues bound to the address. This includes durable, non-durable, statically created, and dynamically (i.e. auto) created queues. Note, this is done on a per-address basis so even though the `match` of the `<address-setting>` may be `#` the `max-size-bytes` applies to each individual address and not every matching address as a whole.
* **address-full-policy** What to do when the address becomes full.  Options are:
  * **PAGE** messages will be stored on disk
  * **DROP** messages will be silently dropped (i.e. deleted)
  * **FAIL** messages will be dropped and the relevant producer will receive an error
  * **BLOCK** relevant producer will be blocked from sending additional messages; only clients which support flow-control (i.e. core JMS, AMQP) will block

Aside from the per-address limits there are also global limits:

* **global-max-size** When all the message data in the broker reaches this limit then all addresses will apply their respective `<address-full-policy>`.  Measured in bytes; supports byte notation (e.g. Mb, GB, kb, etc.). Defaults to Xmx/2.
* **max-disk-usage** When disk utilization reaches this percentage (for any reason) then all clients supporting flow control will be blocked and those that don't will be disconnected.

These are the parts of the default configuration relevant to this discussion:

```xml
<max-disk-usage>90</max-disk-usage>

<global-max-size>100Mb</global-max-size>

<address-setting match="#">
   <dead-letter-address>DLQ</dead-letter-address>
   <expiry-address>ExpiryQueue</expiry-address>
   <redelivery-delay>0</redelivery-delay>
   <!-- with -1 only the global-max-size is in use for limiting -->
   <max-size-bytes>-1</max-size-bytes>
   <message-counter-history-day-limit>10</message-counter-history-day-limit>
   <address-full-policy>PAGE</address-full-policy>
   <auto-create-queues>true</auto-create-queues>
   <auto-create-addresses>true</auto-create-addresses>
   <auto-create-jms-queues>true</auto-create-jms-queues>
   <auto-create-jms-topics>true</auto-create-jms-topics>
</address-setting>
```

### Hands on

Create a fresh broker instance:

```
$ <AMQ_HOME>/bin/artemis create --user <myuser> --password <mypassword> --role admin --require-login <AMQ_HOME>/instances/myprotectedbroker
```

Open `AMQ_INSTANCE/etc/broker.xml` and change `<max-disk-usage>` as the default value (i.e. 90) can inadvertently trigger blocking if your disk is close to full:

```xml
<max-disk-usage>100</max-disk-usage>
```

Then change the `<address-full-policy>` for `<address-setting match="#">` to: 

```xml
<address-full-policy>BLOCK</address-full-policy>
```

And also change the `<max-size-bytes>` to:

```xml
<max-size-bytes>1MB</max-size-bytes>
```

Now send messages to the broker:

```
$ <AMQ_INSTANCE>/bin/artemis producer --user <myuser> --password <mypassword> --message-size 1050
```

The producer should block with this:

```
AMQ212054: Destination address=TEST is blocked. If the system is configured to block make sure you consume messages on this configuration.
```

Kill the producer (e.g. using Ctrl-C), and consume all the messages:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --user <myuser> --password <mypassword> --receive-timeout 1000 --break-on-null
```

Check the AMQ 7 log and you'll see where the broker blocked and unblocked during the exercise.

## Resource limits

Broker administrators may be concerned with how many connections or queues a particular user is allowed to create.  This can be configured via `<resource-limit-settings>`, e.g.:

```xml
<resource-limit-settings>
   <resource-limit-setting match="myuser">
      <max-queues>100</max-queues>
      <max-connections>10</max-connections>
   </resource-limit-setting>
</resource-limit-settings>
```

The `match` attribute on `<resource-limit-setting>` matches a username.  Wildcards are not supported on this match.  The `<max-queues>` is how many queues the user is allowed to create either manually or via auto-creation.  The `<max-connections>` is how many connections the user is allowed to create.

## Hands on

Add this to your `AMQ_INSTANCE/etc/broker.xml`:

```xml
<resource-limit-settings>
   <resource-limit-setting match="myuser">
      <max-queues>0</max-queues>
   </resource-limit-setting>
</resource-limit-settings>
```

Restart the broker (because `<resource-limit-settings>` is not automatically picked up when changed at runtime) and try to send messages:

```
$ <AMQ_INSTANCE>/bin/artemis producer --user <myuser> --password <mypassword>
```

This should fail since the producer will attempt to create the queue "TEST" by default:

```
javax.jms.JMSException: AMQ119111: Too many queues created by user '<myuser>'. Queues allowed: 0.
```

Now change the `<resource-limit-settings>`:

```xml
<resource-limit-settings>
   <resource-limit-setting match="myuser">
      <max-queues>10</max-queues>
      <max-connections>3</max-connections>
   </resource-limit-setting>
</resource-limit-settings>
```

Run a consumer with 2 threads.  Note, even though there's just 2 threads the core JMS client will actually create 3 core sessions - 1 for the JMS connection and 1 each for the JMS sessions (1 per thread) for a total of 3.

```
$ <AMQ_INSTANCE>/bin/artemis consumer --user <myuser> --password <mypassword> --threads 2 --receive-timeout 100 --break-on-null
```

This will run without problems.  However, a consumer with 3 threads (i.e. 4 core sessions) will fail:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --user <myuser> --password <mypassword> --threads 3 --receive-timeout 100 --break-on-null
```

Here's the output:

```
javax.jms.JMSException: AMQ119110: Too many sessions for user '<myuser>'. Sessions allowed: 3.
```

## Limiting Connections on an Acceptor

Instead of limiting connections on a user-by-user basis you can also apply a global limit on connections to an acceptor using the `connectionsAllowed` URL property.

### Hands on

Stop your broker instance, remove the `<resource-limit-settings>`, and change the `artemis` acceptor to this (adding `;connectionsAllowed=1` to the end):

```xml
<acceptor name="artemis">tcp://0.0.0.0:61616?tcpSendBufferSize=1048576;tcpReceiveBufferSize=1048576;protocols=CORE,AMQP,STOMP,HORNETQ,MQTT,OPENWIRE;useEpoll=true;amqpCredits=1000;amqpLowCredits=300;connectionsAllowed=1</acceptor>
```

Restart the broker and connect a consumer:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --user myuser --password mypass
```

This will succeed. Note, even though the consumer is creating multiple core sessions it's only using a single TCP connection.

Connect another consumer:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --user myuser --password mypass
```

This will fail after a bit and ask you for a working URL.  The broker will report something like this:

```
AMQ222206: Connection limit of 1 reached. Refusing connection from /127.0.0.1:57918
```

The broker here is simply closing the TCP connection which may result in different behavior in the different supported clients.
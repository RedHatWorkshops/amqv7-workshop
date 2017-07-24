# Advanced Features in AMQ 7 broker

This worksheet covers some advanced features in the AMQ 7 broker.  By the end of this worksheet you should know:

1. How a "last-value" queue works
2. How to configure a dead-letter address
3. How slow-consumer detection works

## Last-Value Queues

A Last-Value queue (henceforth LVQ) is a special queue which discards any messages when a newer message with the same value for a well-defined Last-Value property (i.e. `_AMQ_LVQ_NAME`) is put in the queue.  In other words, an LVQ queue only retains the last value.

This functionality is configured via the `<last-value-queue>` element specified in a matching `<address-setting>`.  For example, given this address & queue:
 
```xml
<addresses>
 <address name="lastValue">
   <anycast>
     <queue name="lastValueQueue"/>
   </anycast>
 </address>
</addresses>
```

This `<address-setting>` would enable LVQ semantics:

```xml
<address-setting match="lastValue">
   <last-value-queue>true</last-value-queue>
</address-setting>
```

Add this address & queue to your broker.xml (but not the `<address-setting>`).

Now run the LVQ example to send 3 messages which have the same value for `_AMQ_LVQ_NAME`:

```
mvn verify -PLVQ
```

Now consume those messages:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --destination lastValueQueue --receive-timeout 1000 --break-on-null
```

Three messages will be consumed.

Now we want to enable LVQ semantics on the queue we created.  However, because the runtime configuration logic only supports non-destructive changes we have to stop the broker and clear the journal using this command:

```sh
rm -Rf AMQ_INSTANCE/data/
```

Then add the aforementioned `<address-setting>` to broker.xml, start the broker, and run the sender again:

```
mvn verify -PLVQ
```

Now consume those messages:

```
$ <AMQ_INSTANCE>/bin/artemis consumer --destination lastValueQueue --receive-timeout 1000 --break-on-null
```

Only 1 message (the last one - #3) will be consumed.

It's important to note that LVQ is not currently supported for AMQP clients.

## Dead-letter address

A "dead-letter" destination is a common concept in messaging.  Simply put, it's a place where messages are sent when a client fails to consume them.  In AMQ 7 this behavior is configured via `<address-setting>`.

Here is one of the default `<address-setting>` elements:

```xml
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

Notice the `<dead-letter-address>` attribute.  It tells the broker where to send messages which clients fail to consume.  Other related attributes are:

* **max-delivery-attempts** How many time a cancelled message can be redelivered before sending to the dead-letter-address.
* **redelivery-delay** How long to wait before attempting redelivery of a cancelled message.
* **redelivery-delay-multiplier** This multiplier enables subsequent redelivery delays to be larger. The formula used here is (redlivery-delay * (redlivery-delay-multiplier ^ (delivery-count - 1))). 
* **max-redelivery-delay** Sets are hard limit on the maximum redelivery delay which may be useful when using `redelivery-delay-multiplier` and a large `max-delivery-attempts`.
* **send-to-dla-on-no-route** If a message is sent to an address, but the server does not route it to any queues, for example, there might be no queues bound to that address, or none of the queues have filters that match, then normally that message would be discarded. However if this parameter is set to true for that address, if the message is not routed to any queues it will instead be sent to the dead letter address (DLA) for that address, if it exists.

Here's a simple example that combines most of these attributes:

```xml
<address-setting match="#">
   <dead-letter-address>DLQ</dead-letter-address>
   <redelivery-delay>1000</redelivery-delay>
   <redelivery-delay-multiplier>2</redelivery-delay-multiplier>
   <max-redelivery-delay>10000</max-redelivery-delay>
   <max-delivery-attempts>6</max-delivery-attempts>
</address-setting>
```

Consider a broken client who is simply unable to consume a message successfully.  This broken consumer is listening to a queue which has 1 message.  Here is how the sequence of delivery attempts would go given the `<address-setting>` above:

1. no delay on the first attempt
2. delivery-count = 1, delay of 1 second (1000 * (2.0 ^ 0))
3. delivery-count = 2, delay of 2 seconds (1000 * (2.0 ^ 1))
4. delivery-count = 3, delay of 4 seconds (1000 * (2.0 ^ 2))
5. delivery-count = 4, delay of 8 seconds (1000 * (2.0 ^ 3))
6. delivery-count = 5, delay of 10 seconds since (1000 * (2.0 ^ 4)) is > 10000 which is the max-redelivery-delay
7. delivery-count = 6, send to the dead-letter address "DLQ"

## Slow-consumer detection

A slow consumer with a server-side queue (e.g. JMS topic subscriber) can pose a significant problem for broker performance. If messages build up in the consumer's server-side queue then memory will begin filling up and the broker may enter paging mode which would impact performance negatively. However, criteria can be set so that consumers which don't acknowledge messages quickly enough can potentially be disconnected from the broker which in the case of a non-durable JMS subscriber would allow the broker to remove the subscription and all of its messages freeing up valuable server resources.

Slow-consumer detection is configured via the following `<address-setting>` attributes:
 
* **slow-consumer-threshold** The minimum rate of message consumption allowed before a consumer is considered "slow." Measured in messages-per-second. Default is -1 (i.e. disabled); any other valid value must be greater than 0.
* **slow-consumer-policy** What should happen when a slow consumer is detected. KILL will kill the consumer's connection (which will obviously impact any other client threads using that same connection). NOTIFY will send a CONSUMER_SLOW management notification which an application could receive and take action with.
* **slow-consumer-check-period** How often to check for slow consumers on a particular queue. Measured in seconds. Default is 5.
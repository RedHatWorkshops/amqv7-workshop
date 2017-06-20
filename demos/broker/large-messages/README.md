## Large Messages in AMQ7 Broker   

This worksheet covers handling large messages in the AMQ7 Broker. 
By the end of this you should know:

1. The large message concepts of AMQ7
   * Memory Limits in the Broker
   * Sending Large Messages
   * Streaming large messages
   
2. How to configure Large Messages
   * Configuring a broker to handle large messages
   * Configuring a client to use Large Messages
   * Configuring a client to stream messages


### AMQ7 Large Message Concepts

There are 2 main issues that can happen when you send or receive messages to an AMQ 7 Broker.

#### Memory Limits within the broker

Any message that is sent or consumed from a Broker has at some point to exist in memory before
it can either be persisted to disc after receipt or delivered to a consumer. This can be problematic 
when running in an environment with limited memory. The AMQ7 Broker handles large messages by never 
 fully reading them into memory, instead the broker uses its large message store to hold the contents 
 of the message. The location of this is configured in the broker.xml file like so:
  
```xml
      <large-messages-directory>largemessagesdir</large-messages-directory>
```
This is always configured by default.

#### Large Messages and Clients
  
    > **Note**
    >
    > Currently only the Core JMS client can handle large messages

##### Sending a large message

To configure the core JMS client to send a message you need to configure min-large-message-size on 
the connection factory. This can be configured in multiple ways.

If JNDI is used to instantiate and look up the connection factory, the minimum large message size is configured in the 
JNDI context environment, e.g. `jndi.properties`. Here's a simple example using the "ConnectionFactory" connection factory 
which is available in the context by default:

    java.naming.factory.initial=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory
    connectionFactory.myConnectionFactory=tcp://localhost:61616?minLargeMessageSize=10240


If the connection factory is being instantiated directly in Java, the minimum
large message size is specified by

   `ActiveMQConnectionFactory.setMinLargeMessageSize`.

To see this in action start a Broker in the usual fashion.
 
Use the 'com.redhat.workshop.amq7.largemessage.Sender'to send a large message like so:

      mvn verify -PLargeMessageSender
   
Since the minLargeMessageSize size in the example is set to 10240 bytes and the Sender Client is sending
a bytes message of size 20480 bytes, the client will treat this as a large message and send it in chunks. 

The Broker will then write these chunks to its large message store without having to keep them in memory.
Obviously in reality the large message can be as large as the client can fir into memory.

Take a look in the large message store and you will see the large message file.

You can consume the message using the 'com.redhat.workshop.amq7.largemessage.Receiver' client by running:
 
       mvn verify -PLargeMessageReceiver
       
Note that the large message file has now disappeared.
       
#### Streaming a large message

If Client memory is also constrained then it is possible to stream a message straight from disc 
  or another type of Input Stream. This can only be done via the JMS API by sending a 
  JMS BytesMessage and setting an input stream as an Object property, like so
  
```java
  BytesMessage bytesMessage = session.createBytesMessage();
  FileInputStream fileInputStream = new FileInputStream(inputFile);
  BufferedInputStream bufferedInput = new BufferedInputStream(fileInputStream);
  bytesMessage.setObjectProperty("JMS_AMQ_InputStream", bufferedInput);
```

The Client will stream the contents of the file direct to the TCP stream in chunks.
This is then handled by the Broker as a large message. To see this in action use the 
''com.redhat.workshop.amq7.streammessage.Sender'to send a stream message like so:'

   mvn verify -PStreamMessageSender

Inspect the large message store and you will again see the large message file.

To consume a stream message you simple consume a JMS BytesMessage with an OutputStream using the
 JMS API like so:
 
 
```java
   BytesMessage messageReceived = (BytesMessage) messageConsumer.receive(120000);
   File outputFile = new File("huge_message_received.dat");
   FileOutputStream fileOutputStream = new FileOutputStream(outputFile)
   BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);

   //This will save the stream and wait until the entire message is written before continuing.
   messageReceived.setObjectProperty("JMS_AMQ_SaveStream", bufferedOutput);
```

To see this in action use the 
''com.redhat.workshop.amq7.streammessage.Receiver'to receive a stream message like so:'

   mvn verify -PStreamMessageReceive
   
    > **Note**
    >
    > Stream messages and large messages are interchangeable so you can send a large
    > message and consume a stream message and vice versa.
   

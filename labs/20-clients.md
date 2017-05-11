# AMQ Clients

AMQ 7 comes with several new clients covering a variety of protocols and programming languages. In the past, any supported clients were only released when we released new versions of the AMQ product. With AMQ7 that has changed; each client has its own lifecycle and is released independently from the AMQ broker/server components. 

AMQ7 includes the following supported clients:

* AMQ JMS Client ([official docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_jms_client/))
* AMQ C++ Client ([official docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_cpp_client/))
* AMQ JavaScript Client ([official docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_javascript_client/))
* AMQ Python Client ([official docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_python_client/))
* AMQ .NET Client ([official docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_.net_client/))

Let's explore the clients.

## AMQ JMS Client

To use the AMQ JMS client, navigate to [https://developers.redhat.com/products/amq/download/](https://developers.redhat.com/products/amq/download/) and locate the AMQ JMS Client.

![Download JMS](images/client/download-jms-client.png)

When you've downloaded the file, copy it to a location where you'd like to unzip this client.

    $ mv ~/Downloads/apache-qpid-jms-0.21.0.redhat-1-bin.zip ./clients/
    $ cd clients
    $ unzip apache-qpid-jms-0.21.0.redhat-1-bin.zip
    $ cd apache-qpid-jms-0.21.0.redhat-1
    
    
Now we'll explore the client. In the distribution we just unzipped, there's an `examples` folder. Navigate into it and you'll find a Java Maven project. Let's build the project:


    $ cd examples
    $ mvn clean install
    
If successful, you should see output like this:
    
```
[INFO] 
[INFO] --- maven-install-plugin:2.5.2:install (default-install) @ qpid-jms-examples ---
[INFO] Skipping artifact installation
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 4.063 s
[INFO] Finished at: 2017-05-11T15:52:15-07:00
[INFO] Final Memory: 28M/456M
[INFO] ------------------------------------------------------------------------
```    

Feel free to open this project in your favorite IDE (like [JBoss Developer Studio](https://developers.redhat.com/products/devstudio/overview/)). 

A couple of things to notice about this example:

* Our JMS ConnectionFactory and destination names are configured in the `src/main/resources/jndi.properties` file
* Our `org.apache.qpid.jms.example.HelloWorld` main() class bootstraps the `MessageConsumer` and `MessageProducer` used in this example
* We lookup the JMS connection information from JNDI as specified in our `jndi.properties` file
* We send one message and receive one message.

Review the code closer to get an idea of what it's doing:

```java
    public static void main(String[] args) throws Exception {
        try {
            // The configuration for the Qpid InitialContextFactory has been supplied in
            // a jndi.properties file in the classpath, which results in it being picked
            // up automatically by the InitialContext constructor.
            Context context = new InitialContext();

            ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
            Destination queue = (Destination) context.lookup("myQueueLookup");

            Connection connection = factory.createConnection(System.getProperty("USER"), System.getProperty("PASSWORD"));
            connection.setExceptionListener(new MyExceptionListener());
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer messageProducer = session.createProducer(queue);
            MessageConsumer messageConsumer = session.createConsumer(queue);

            TextMessage message = session.createTextMessage("Hello world!");
            messageProducer.send(message, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
            TextMessage receivedMessage = (TextMessage) messageConsumer.receive(2000L);

            if (receivedMessage != null) {
                System.out.println(receivedMessage.getText());
            } else {
                System.out.println("No message received within the given timeout!");
            }

            connection.close();
        } catch (Exception exp) {
            System.out.println("Caught exception, exiting.");
            exp.printStackTrace(System.out);
            System.exit(1);
        }
    }
```

To run this example, we'll first download all of the project's dependencies:

    $ mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DskipTests
    
Next, we need to make sure our broker is running. If you've followed from the previous labs, you have a broker running with a single `Acceptor` running on port `61616`. Let's change our `jndi.properties` file to reflect this change. (if you're coming to this lab from your own installation/running instance of the AMQ7 broker, adjust the properties as needed; e.g., if you still have the dedicated AMQP `Acceptor` running, then no need to make this change). 

Our `jndi.properties` file should look like this, with the `connectionfactory.myFactoryLookup` property set to `61616`

```
# Set the InitialContextFactory class to use
java.naming.factory.initial = org.apache.qpid.jms.jndi.JmsInitialContextFactory

# Define the required ConnectionFactory instances
# connectionfactory.<JNDI-lookup-name> = <URI>
connectionfactory.myFactoryLookup = amqp://localhost:61616

# Configure the necessary Queue and Topic objects
# queue.<JNDI-lookup-name> = <queue-name>
# topic.<JNDI-lookup-name> = <topic-name>
queue.myQueueLookup = queue
topic.myTopicLookup = topic
```
    
Now let's build our project and run:

    $ mvn clean install
    $ java -cp "target/classes/:target/dependency/*" org.apache.qpid.jms.example.HelloWorld
    
If everything completed properly, you should see the following output:

```
Hello world!
```

#### Some things to note:

The URL we passed to the connection factory should be in the following form:

> amqp[s]://hostname:port[?option=value[&option2=value...]]

We can also use the failover URI (discussed in future lab) like this:

> failover:(amqp://host1:port[,amqp://host2:port...])[?option=value[&option2=value...]]

Options for configuration can be [found at the AMQ7 JMS Client product documentation](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_the_amq_jms_client/configuration#connection_uri_options_jms)


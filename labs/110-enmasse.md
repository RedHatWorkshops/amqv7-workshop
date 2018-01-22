# Messaging as a Service with Enmasse

Red Hat AMQ Online is a new Messaging as a Service (MaaS) offering built on OpenShift.  It offers a self-service approach to managing brokers, giving developers the ability to create and manage their own queues and topics.  This lab steps you through this process using the technical preview environment from Red Hat.

To get started:

1. Request an Red Hat AMQ Online account by [email](amq-online-tech-preview-owner@redhat.com).
2. Login to Enmasse using the credentials given in the response email.  Ensure you can login and view the Dashboard
3. Click on "Addresses", then click "Create".  Create a new address called "queue" using the standard plan
4. Git clone the qpid-jms project from [here](https://github.com/apache/qpid-jms) to a local directory
5. Update the `jndi.properties` file located in `qpid-jms-examples/src/main/resources/jndi.properties` with the following:

```
connectionfactory.myFactoryLookup = amqps://<insert your URL>?transport.trustAll=true&transport.verifyHost=false
queue.myQueueLookup = queue
```
6. Via the command line, execute the following command in the `qpid-jms-examples` directory:

```
java -cp "target/classes/:target/dependency/*" -DUSER="<insert your AMQ online user>" -DPASSWORD="<insert your AMQ online password>" org.apache.qpid.jms.example.Sender
```
7. Check the Enmasse dashboard and it's been updated with the messages sent to "queue".

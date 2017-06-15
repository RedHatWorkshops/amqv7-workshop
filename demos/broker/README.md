# Getting started
## Install prerequisites

JDK 1.8
 
OpenJDK on Fedora
    
    yum install java-1.8.0-openjdk-devel
    yun install java-1.8.0-openjdk
    
Oracle JDK with  Mac

    http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
    
Maven 3

Download 3.3.9 from https://maven.apache.org/download.cgi ans unzip somewhere.
make sure it is available on your path

Git client

Fedora
    
    yum install git on Fedora

Mac 
    follow instructions at https://git-scm.com/book/en/v2/Getting-Started-Installing-Git
    

LibAIO (optional Fedora only) - Fedora - 'yum install libaio'

### Install Broker

Download the latest version of the Broker from https://developers.redhat.com/products/amq/download/.

Follow the install instructions at https://developers.redhat.com/products/amq/hello-world and create and run an AMQ 7 Broker
instance.

Goto http://localhost:8161/hawtio/login and log in using the default user/pass you created when the A-MQ7 instance was created.

### Using a Queue

-   Stop the Broker
-   Add an anycast type address with a queue 
```xml 
    <addresses>
     <address name="exampleQueue">
       <anycast>
         <queue name="exampleQueue"/>
       </anycast>
     </address>
    </addresses>
```

-   Start The broker
-   Use the Artemis CLI to listen for some messages
```code
ARTEMIS_HOME/bin/artemis consumer --destination queue://exampleQueue
```
-   then send some messages with the CLI
```code
ARTEMIS_HOME/bin/artemis producer --destination queue://exampleQueue
```
### Using a topic

-   Stop the Broker
-   Add a multicast type address with a topic 
```xml 
  <addresses>
    <address name="exampleTopic">
      <multicast/>
    </address>
  </addresses>
```

-   Start The broker

-   listen for some messages

```code
ARTEMIS_HOME/bin/artemis consumer --destination topic://exampleTopic
```         

-   send some messages

```code
ARTEMIS_HOME/bin/artemis producer --destination topic://exampleTopic
```
   


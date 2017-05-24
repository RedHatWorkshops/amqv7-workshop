# Persistence / Master-Slave Jounraling

Many use cases involving a messaging system require some form of delivery of messages even in the face of planned or unplanned failures. AMQ7 solves this by persisting messages in some form. In this lab, we'll look at a well-known and often practiced deployment for achieving message durability and delivery using a shared file system and two or more brokers set up in a master / slave topology. In the [next lab](50-replication.md) we'll see how to accomplish the same goals without a shared file system. 


AMQ7 can be set up in a shared-file system type of deployment for master/slave. To see this in action, we'll spend a moment talking about the messaging journal.

There are actually a couple different journal files used for different purposes. If we look at the configuration file in `./etc/broker.xml` of our `myfirstbroker` folder (if you've been following along in the previous labs) find the following configuration entries:


```xml
  <persistence-enabled>true</persistence-enabled>

  <!-- this could be ASYNCIO or NIO
   -->
  <journal-type>NIO</journal-type>

  <paging-directory>./data/paging</paging-directory>

  <bindings-directory>./data/bindings</bindings-directory>

  <journal-directory>./data/journal</journal-directory>

  <large-messages-directory>./data/large-messages</large-messages-directory>

  <journal-datasync>true</journal-datasync>

  <journal-min-files>2</journal-min-files>

  <journal-pool-files>-1</journal-pool-files>
```
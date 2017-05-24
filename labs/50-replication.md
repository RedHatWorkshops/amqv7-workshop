# AMQ 7 Message Persistence & Replication

AMQ7 has various message persistence options:

* NIO (Java NIO, journal-based persistence)
* ASYNCIO (Linux Asynchronous IO, journal-based persistence)
* JDBC (persist to the relational database of your choice)
* Memory (in-memory stateless message persistence)

The default, out-of-the-box settin is NIO file journal-based persistence.  You can configure message persistence by updating the `/jboss-amq-7.0.0.redhat-1/brokers/myfirstbroker/etc/broker.xml` file.  Here is the section in `broker.xml`:

```
<configuration>
  <core>
    ...
	  <persistence-enabled>true</persistence-enabled>
    <!-- this could be ASYNCIO or NIO-->
    <journal-type>NIO</journal-type>
    <paging-directory>./data/paging</paging-directory>
    <bindings-directory>./data/bindings</bindings-directory>
    <journal-directory>./data/journal</journal-directory>
    <large-messages-directory>./data/large-messages</large-messages-directory>
    <journal-datasync>true</journal-datasync>
    <journal-min-files>2</journal-min-files>
    <journal-pool-files>-1</journal-pool-files>
    ...
  </core>
</configuration>
```

For further details around message persistence, refer to the product documentation [here](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_amq_broker/basic_configuration#configuring_persistence).

## Replication





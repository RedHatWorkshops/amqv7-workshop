# Master/Slave - Shared Store

A-MQ 7 has the ability to run in a master/slave (ie, active/passive) configuration so that if an active node goes down, another can immediately take its place and continue serving clients and processing messages. There are a few different options that can be used to achieve this:

* Shared store
* Replicated store (ie, "shared nothing")
* Co-located

For the purpose of this lab, we will focus on the "Shared store" option for persistence, and use the "Static" option for clustering.

## Prerequisites

Download and install the A-MQ 7 broker package as described in [Lab 1](00-install-broker.md).

## Creating the brokers

```
$ $AMQ_HOME/bin/artemis create brokers/master --name master --user admin --password admin --allow-anonymous
$ $AMQ_HOME/bin/artemis create brokers/slave --name slave --user admin --password admin --allow-anonymous --port-offset 1
```

_Note: We could have passed in some extra arguments to configure our master/slave pair with some defaults, but instead we're going to manually configure them via the `broker.xml` file so we can become familiar with the available options._

## Editing the configuration

__Master__

1. Open up the `brokers/master/etc/broker.xml` file in your favorite text editor.

2. Modify the existing `paging-directory`, `bindings-directory`, `journal-directory`, and `large-messages-directory` elements to point to a shared mount. In the real world, this would likely point to a directory on a SAN that is mounted on both the master and slave machines.

```xml
<paging-directory>../sharedstore/data/paging</paging-directory>
<bindings-directory>../sharedstore/data/bindings</bindings-directory>
<journal-directory>../sharedstore/data/journal</journal-directory>
<large-messages-directory>../sharedstore/data/large-messages</large-messages-directory>
```

3. Add the following elements anywhere under the `<core>` element:

```xml
<connectors>
  <connector name="master-connector">tcp://localhost:61616</connector>
  <connector name="slave-connector">tcp://localhost:61617</connector>
</connectors>

<cluster-user>admin</cluster-user>
<cluster-password>admin</cluster-password>

<cluster-connections>
  <cluster-connection name="static-cluster">
    <connector-ref>master-connector</connector-ref>
    <static-connectors>
      <connector-ref>slave-connector</connector-ref>
    </static-connectors>
  </cluster-connection>
</cluster-connections>

<ha-policy>
  <shared-store>
    <master>
      <failover-on-shutdown>true</failover-on-shutdown>
    </master>
  </shared-store>
</ha-policy>
```

4. Start the broker:

```
$ ./brokers/node1/bin/artemis run
```

__Slave__

1. Open up the `brokers/slave/etc/broker.xml` file in your favorite text editor.

2. Modify the existing `paging-directory`, `bindings-directory`, `journal-directory`, and `large-messages-directory` elements to point to a shared mount. In the real world, this would likely point to a directory on a SAN that is mounted on both the master and slave machines.

```xml
<paging-directory>../sharedstore/data/paging</paging-directory>
<bindings-directory>../sharedstore/data/bindings</bindings-directory>
<journal-directory>../sharedstore/data/journal</journal-directory>
<large-messages-directory>../sharedstore/data/large-messages</large-messages-directory>
```

3. Add the following elements anywhere under the `<core>` element:

```xml
<connectors>
  <connector name="master-connector">tcp://localhost:61616</connector>
  <connector name="slave-connector">tcp://localhost:61617</connector>
</connectors>

<cluster-user>admin</cluster-user>
<cluster-password>admin</cluster-password>

<cluster-connections>
  <cluster-connection name="static-cluster">
    <connector-ref>slave-connector</connector-ref>
    <static-connectors>
      <connector-ref>master-connector</connector-ref>
    </static-connectors>
  </cluster-connection>
</cluster-connections>

<ha-policy>
  <shared-store>
    <slave>
      <failover-on-shutdown>true</failover-on-shutdown>
      <allow-failback>true</allow-failback>
    </slave>
  </shared-store>
</ha-policy>
```

4. Start the broker:

```
$ ./brokers/node2/bin/artemis run
```

## Testing

1. Open up a new terminal window and run the following command:

```
$ $AMQ_HOME/bin/artemis producer --verbose --user admin --password admin --message-count 100 --url 'tcp://localhost:61616'
```

2. Shut down the master broker (ie, by hitting ctrl-c on its terminal). You should see the slave broker log a few messages indicating that it has come online (this might take a couple of seconds).

3. In the same terminal window that you ran the producer command, run the following command:

```
$ $AMQ_HOME/bin/artemis consumer --verbose --user admin --password admin --message-count 100 --url 'tcp://localhost:61617'
```

All of the messages that you produced to the master broker in step 1 should be consumed from the slave broker.

4. (optional) Restart the master broker and you should see that it will take over and the slave will shut down/go back into slave mode (this might take a couple of seconds).

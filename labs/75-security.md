# AMQ 7 Security

One of the key differences with AMQ 7 is the addition of new permissions.  AMQ 6 only had 3 permissions:

1. read
2. write
3. admin

which are described in more detail [here](http://activemq.apache.org/security.html).

AMQ 7 on the other hand extends to 10 permissions.  They are:

* createAddress
* deleteAddress
* createDurableQueue
* deleteDurableQueue
* createNonDurableQueue
* deleteNonDurableQueue
* send
* consume
* manage
* browse

With the additional 7 permissions, we have finer grain control over assigning roles to our users.  For more information about these new roles and how they map to the legacy AMQ 6 roles, please refer to the AMQ 7 [docs](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_amq_broker/security#ldap_authz).

## Security Labs

### Permissions / Roles Lab 1

This lab demonstrates how to setup a read-only user on A-MQ i.e. the user can only consumer from a given queue.

1. Create a new broker by executing the following command:

```
./bin/artemis create
```

Name the broker "securitybroker" and give it an admin user with the credentials admin/admin.

2. `cd` to `brokers/securitybroker/bin`
3. Execute `./artemis user add --user read-only-user --password Abcd1234 --role read-only` to create a read-only user
4. `cd` to `brokers/securitybroker/etc` and open the broker.xml file.
5. Under the security-settings section, add the following text:

```
         <security-setting match="test.#">
            <permission type="createNonDurableQueue" roles="amq"/>
            <permission type="deleteNonDurableQueue" roles="amq"/>
            <permission type="createDurableQueue" roles="amq"/>
            <permission type="deleteDurableQueue" roles="amq"/>
            <permission type="createAddress" roles="amq"/>
            <permission type="deleteAddress" roles="amq"/>
            <permission type="consume" roles="read-only,amq"/>
            <permission type="browse" roles="amq"/>
            <permission type="send" roles="amq"/>
            <!-- we need this otherwise ./artemis data imp wouldn't work -->
            <permission type="manage" roles="amq"/>
         </security-setting>
```
Notice we have a specific match for any queue starting with "test.*".  Also notice that we have assigned the "read-only" role to ensure that our read-only user can only consume from our test.* queue.

5.  Save the broker.xml file

6. Start up our new broker using the following command: `./brokers/securitybroker/bin/artemis run`

7. Try out our new "read only" role / user by typing the following command in a separate command windo:

```
java -jar activemq-all-5.11.0.redhat-630187.jar producer --sleep 100 --messageCount 1000 --user read-only-user --password Abcd1234 --brokerUrl 'failover:(tcp://localhost:61616,tcp://localhost:61617)' --destination queue://test.readonly.queue
```

Notice that this command fails because our read-only-user cannot create a durable queue.  This means our user is working correctly.

8.  Change the command and execute the following instead to use the admin user, which coincidently has admin permissions to create and write to queues:

```
java -jar activemq-all-5.11.0.redhat-630187.jar producer --sleep 100 --messageCount 1000 --user admin --password admin --brokerUrl 'failover:(tcp://localhost:61616,tcp://localhost:61617)' --destination queue://test.readonly.queue
```

9.  Now lets try consuming from the test.readonly.queue using the read-only-user credentials:

```
java -jar activemq-all-5.11.0.redhat-630187.jar consumer --sleep 100 --messageCount 1000 --user read-only-user --password Abcd1234 --brokerUrl 'failover:(tcp://localhost:61616,tcp://localhost:61617)' --destination queue://test.readonly.queue
```

If all goes well, the client should connect and start consuming messages from our queue.

10.  For the sake of testing, try the same to write messages to the queue.  Again, this should fail but with a different permission error:

```
java -jar activemq-all-5.11.0.redhat-630187.jar producer --sleep 100 --messageCount 1000 --user read-only-user --password Abcd1234 --brokerUrl 'failover:(tcp://localhost:61616,tcp://localhost:61617)' --destination queue://test.readonly.queue
```
### SSL

This lab demonstrates generating keys and truststores to use for SSL with the broker.

1. Ensure openssl is installed

```
sudo yum install openssl
```

2. Use openssl to generate the pem and p12 files.  Other file formats can be used by the pem and p12 are compatiable for usage with both the Artemis brokers and then Interconnect Routers

```
openssl req -newkey rsa:2048 -nodes -keyout keyout.pem -x509 -days 65000 -out certificate.pem
openssl x509 -text -noout -in certificate.pem
openssl pkcs12 -inkey keyout.pem -in certificate.pem -export -out certificate.p12
openssl pkcs12 -in certificate.p12 -noout -info
```
Notice this results in the following files
```
keyout.pem
certificate.pem
certificate.p12
```

3. Configure SSL usage on the connectors and acceptors in the broker.xml

```
<acceptors>
    <acceptor name="artemis">tcp://localhost:61616?sslEnabled=true;keyStorePath=certificate.p12;keyStorePassword=password;enabledProtocols=TLSv1,TLSv1.1,TLSv1.2;trustStorePath=certificate.p12;
    trustStorePassword=password</acceptor>
 </acceptors>
 <connectors>
      <connector name="my-connector">tcp://localhost:61616?sslEnabled=true;keyStorePath=certificate.p12;keyStorePassword=password;enabledProtocols=TLSv1,TLSv1.1,TLSv1.2;trustStorePath=certificate.p12;
      trustStorePassword=password</connector>
</connectors>
```

4. If you have multiple brokers configure those as well to ensure any clustering between the brokers will be able to communicate.

5. Start up the brokers, notice that the Cluster Bridge connections should still occur, but this time over SSL

6. Configure your client to use ssl, using a URL similar to the following

```
amqps://localhost:5672?transport.verifyHost=false&transport.storeType=PKCS12&transport.trustStoreLocation=/home/example/broker-secure/certificate.p12&transport.trustStorePassword=password
```

7. Start up your client and see it connect with SSL and consume or produce

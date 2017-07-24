# Security in AMQ 7 broker

This worksheet covers security in the AMQ 7 broker. By the end of this you should know:

1. How the AMQ 7 security architecture compares to AMQ 6
2. How to require authentication and manipulate user data using the default JAAS properties login module
3. How to grant a user access to certain permissions via role-based authorization
4. How to configure SSL

## AMQ 7 vs. AMQ 6

Those familiar with AMQ 6 security will find lots of similarities in AMQ 7.  Both brokers leverage JAAS.  If you're unfamiliar with JAAS you can get a quick overview [here](https://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service).  These JAAS login modules have ported over from AMQ 6 to AMQ 7 with minimal changes:
* Properties files
* LDAP
* SSL certificate
* Guest

The "dual" authentication feature is also present in AMQ 7.  This feature allows independent JAAS domains for SSL and non-SSL connections.

AMQ 6 has these permission types:

* **read**
* **write**
* **admin**

Whereas AMQ 7 has these permission types:

* **createDurableQueue** allows the user to create a durable queue
* **deleteDurableQueue** allows the user to delete a durable queue
* **createNonDurableQueue** allows the user to create a non-durable queue
* **deleteNonDurableQueue** allows the user to delete a non-durable queue
* **send** allows the user to send a message
* **consume** allows the user to consume a message from a queue bound to matching addresses
* **browse** allows the user to browse a queue bound to the matching address
* **manage** allows the user to invoke management operations by sending management messages to the management address

Creating and deleting queues is particularly important in AMQ 7 as that is how subscriptions are implemented.  For example, a JMS client wanting to create a durable subscription would need the `createDurableQueue` permission.

## Authentication

Configuration starts in `AMQ_INSTANCE/etc/bootstrap.xml`.  This is the default configuration:

    <jaas-security domain="PropertiesLogin"/>

The `domain` attribute refers to the relevant login module entry in `AMQ_INSTANCE/etc/login.config`. For example, if an instance were created with the following command:

```
$ <AMQ_HOME>/bin/artemis create --user admin --password password --role admin --allow-anonymous <AMQ_HOME>/instances/mybroker
```

Then this would appear in `login.config`:

    activemq {
       org.apache.activemq.artemis.spi.core.security.jaas.PropertiesLoginModule sufficient
           debug=false
           reload=true
           org.apache.activemq.jaas.properties.user="artemis-users.properties"
           org.apache.activemq.jaas.properties.role="artemis-roles.properties";

       org.apache.activemq.artemis.spi.core.security.jaas.GuestLoginModule sufficient
           debug=false
           org.apache.activemq.jaas.guest.user="admin"
           org.apache.activemq.jaas.guest.role="admin";
    };

The `--allow-anonymous` switch in the `artemis create` command ensures the "guest" login module is added to `login.config` so that anonymous users can access the broker.  By default, anonymous are assigned the user and role also specified in the `artemis create` command via the `user` and `role` switches.  Alternatively one can omit `--allow-anonymous` and specify `--require-login` instead and the "guest" login module will be omitted from `login.config`.

By default user credentials and role information are stored in the properties files `artemis-users.properties` and `artemis-roles.properties` respectively.

The `artemis-users.properties` file consists of a list of properties of the form, `UserName=Password`. For example, to define the users `system`, `user`, and `guest`, you could create a file like the following:

```properties
system=systemPassword
user=userPassword
guest=guestPassword
```

The `artemis-roles.properties` file consists of a list of properties of the form, `Role=UserList`, where UserList is a comma-separated list of users. For example, to define the roles `admins`, `users`, and `guests`, you could create a file like the following:

```properties
admins=system
users=system,user
guests=guest
```


By default passwords are hashed using `PBKDF2WithHmacSHA1`.  However, un-hashed passwords can be manually added if desired by creating an entry without the ENC(<hash>) syntax.

The CLI support commands to manipulate these files, e.g.:

```
$ <AMQ_INSTANCE>/bin/artemis user add|rm|list|reset
```

### Hands on

Create a new broker instance using this command where `<myuser>` and `<mypassword>` are whatever you choose:

```
$ <AMQ_HOME>/bin/artemis create --user <myuser> --password <mypassword> --role admin --require-login <AMQ_HOME>/instances/mysecurebroker
```

Start that instance using this command:

```
$ <AMQ_INSTANCE>/bin/artemis run
```

Run this command, passing in the user and password specified when the instance was created.

```
$ <AMQ_INSTANCE>/bin/artemis producer --message-count 1 --user <myuser> --password <mypassword>
```

Now run it again with a new user and password.

```
$ <AMQ_INSTANCE>/bin/artemis producer --message-count 1 --user <newuser> --password <newpassword>
```

This will fail because the broker doesn't recognize the user.

Add this new user to the broker (make sure the `role` is the same as when the instance was created):

```
$ <AMQ_INSTANCE>/bin/artemis user add --user <newuser> --password <newpassword> --role admin
```

The broker will automatically reload the properties files so that the new user is now valid.  Run this command, passing in the *new* user and *new* password which were just added.

```
$ <AMQ_INSTANCE>/bin/artemis producer --message-count 1 --user <newuser> --password <newpassword>
```

Open `AMQ_INSTANCE/etc/artemis-users.properties` and see the new user and its hashed password.

## Authorization

Authorization is configured in `AMQ_INSTANCE/etc/broker.xml` in the `<security-settings>` element.  Here is the default configuration from an instance created via this command:

```
$ <AMQ_HOME>/bin/artemis create --user admin --password password --role admin --allow-anonymous <AMQ_HOME>/instances/mybroker
```

The role specified in the command via the `role` switch is inserted into the `roles` attribute.

```xml
<security-settings>
   <security-setting match="#">
      <permission type="createNonDurableQueue" roles="admin"/>
      <permission type="deleteNonDurableQueue" roles="admin"/>
      <permission type="createDurableQueue" roles="admin"/>
      <permission type="deleteDurableQueue" roles="admin"/>
      <permission type="createAddress" roles="admin"/>
      <permission type="deleteAddress" roles="admin"/>
      <permission type="consume" roles="admin"/>
      <permission type="browse" roles="admin"/>
      <permission type="send" roles="admin"/>
      <!-- we need this otherwise ./artemis data imp wouldn't work -->
      <permission type="manage" roles="admin"/>
   </security-setting>
</security-settings>
```

A `security-setting` matches one or more addresses via the `match` attribute.  The matching functionality supports wildcards like `#` which signifies "any sequence of words."  See [the documentation](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_amq_broker/addresses#wildcard_syntax) for more details about the wildcard syntax.

Since the default `security-setting` matches `#` and refers only to the `admin` role that means only users in the `admin` role can perform any real work.

### Hands on

Remove the user you created previously (which was in the `admin` role):

```
$ <AMQ_INSTANCE>/bin/artemis user rm --user <newuser>
```

Recreate that user in a different role:

```
$ <AMQ_INSTANCE>/bin/artemis user add --user <newuser> --password <newpassword> --role <newrole>
```

Attempt to send a message with this user:

```
$ <AMQ_INSTANCE>/bin/artemis producer --message-count 1 --user <newuser> --password <newpassword>
```

You should see an error with something like this:

```
User: <newuser> does not have permission='SEND' on address TEST
```

Modify the `security-setting` to allow your new user to send:

```xml
<permission type="send" roles="admin,<newrole>"/>
```

The running broker will automatically reload the modified `security-settings`.  Now attempt to send again:

```
$ <AMQ_INSTANCE>/bin/artemis producer --message-count 1 --user <newuser> --password <newpassword>
```

The send will now succeed.

## SSL

Both one-way and two-way SSL are supported.  

SSL artifact configuration can be done a few different ways:

* Standard system properties:

```text
javax.net.ssl.keyStore
javax.net.ssl.keyStorePassword
javax.net.ssl.trustStore
javax.net.ssl.trustStorePassword
```

* Broker-specific system properties (will override standard system properties):

```text
org.apache.activemq.ssl.keyStore
org.apache.activemq.ssl.keyStorePassword
org.apache.activemq.ssl.trurestStore
org.apache.activemq.ssl.trustStorePassword
```

* URL (either on a connector or acceptor):

tcp://localhost:5500?sslEnabled=true;__keyStorePath__=../etc/activemq.example.keystore;__keyStorePassword__=activemqexample;__trustStorePath__=../etc/activemq.example.truststore;__trustStorePassword__=activemqexample

Regardless of how the SSL artifacts are configured the `sslEnabled` URL property must always be `true`.

An acceptor can require a client-side certificate by setting the `needClientAuth` URL property to `true`.

## Misc

To disable security completely simply set the `security-enabled` property to false in the `broker.xml` file.

For performance reasons security is cached and invalidated every so long. To change this period set the property `security-invalidation-interval`, which is in milliseconds. The default is `10000` ms.

Password masking is supported.

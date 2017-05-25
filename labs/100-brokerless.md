# Brokerless routing

Although the Interconnect Router can be used to build large-scale, geographically distributed broker clusters, it also provides a mechanism to to inter-network RPC against a transparent backplane where producers and consumers are decoupled in location (just like with a broker, but without a broker). This gives some of the best of both the messaging and RPC worlds. Let's take a look.



In this lab we'll take a look at configuring the Interconnect Router to route messages to and from an AMQ7 broker. Clients can talk directly to the router and think they're talking to a broker.
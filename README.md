# JBoss A-MQ 7 Workshop


This workshop introduces participants to JBoss AMQ 7 through a presentation and hands-on lab format. The workshop is intended to be delivered in person, but will provide enough guidance for self-paced consumption.

## Agenda

A rough agenda for the workshop looks like this:

* High-level overview of messaging landscape
    * Why messaging
    * How it's important to cloud-native applications
    * JMS
    * AMQP
    * Kafka-like messaging
    * Messaging as a Service

* Why AMQ 7
    * Strategic direction of AMQ 7
    * Use cases
    * High-level functionality/features

* Quick intro demo (sending messages with various types of clients)

* AMQ 7 broker installation and hello world (Lab 0)

* Deep Dive AMQ 7 broker
    * Architecture
    * Broker
        * Configuration
        * Clients
        * Persistence
        * Clustering
        * Discovery
    * AMQP
    * High availability
    * Scaling
    * Complex routing
    * Management and Monitoring
    * Security

* Quick intermediate demo (HA/Failover)

* Deep Dive AMQ 7 Interconnect router
    * What is Interconnect router?
    * Routing
        * End to end delivery settlement
        * Smart/adaptive routing
    * Topologies
    * Configuration
    * Broker + Router

* AMQ 7 Interconnect router demo

* Hands-on Labs

## Slides

The slides are written in [RevealJS](http://lab.hakim.se/reveal-js/#/)/[Hyla](https://github.com/cmoulliard/hyla) which is basically a text (asciidoc) format and then converted into an HTML 5 slideshow presentation. 

* [High-level overview of messaging landscape](slides/landscape.md)
* [Why AMQ 7](slides/why-amq7.md)
* [Deep Dive AMQ 7 broker](slides/deep-dive-broker.md)
* [Deep Dive AMQ 7 interconnect router](slides/deep-dive-qdr.md)

## Labs

The labs are written in [Gitbook](https://www.gitbook.com) format and can be viewed online or in offline book (pdf, epub, mobi) format. 

See the latest [lab guide](https://redhatworkshops.github.io/amqv7-workshop/index.html) for the lab table of contents;

## Usecases
The use cases are expanded deliverables based on the Summit presentation by Christina Lin and cover the following use cases.

Trello Project Tracking Link —> https://trello.com/b/NOUVUMcF/a-mq7-delivery-workshop-development
* [Usecase 01] Telecom Service Provider Topology
* [Usecase 02] Banking Service Topology
* [Usecase 03] Healthcare Institution Topology
* [Usecase 04] Call Center Topology
* [Usecase 05] Transportation Ticketing Service Topology
* [Usecase 06] Online Retail Service Toplology

## Contributing

We welcome all forms of contribution (content, issues/bugs, feedback). Please see the [Contribution guidelines for ways to help](./CONTRIBUTING.md)
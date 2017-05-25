# Installing and Configuring AMQ7 Interconnect Router

The AMQ7 Interconnect Router components are written in native code (C++) and are supported on RHEL. In this lab, we'll take a look at two ways to bootstrap Interconnect Router for the purposes of demonstrating routing and broker to router connectivity. If you're looking to do an installation of Interconnect Router on your machines (ie, not just for following this lab), then the official documentation is quite good. [Check it out at access.redhat.com](https://access.redhat.com/documentation/en-us/red_hat_jboss_amq/7.0/html/using_amq_interconnect/installation) for doing a proper installation.


### Option 1 Install into a fresh copy of RHEL 7

Option one requires you to have a RHEL host already provisioned. Either you can use one you already have, or for the purposes of this lab you can install a developer copy. If you go to [http://developers.redhat.com](http://developers.redhat.com), you'll notice lots of helpful tools for developers including e-books, cheatsheets, and community software. Go to the [Red Hat Enterprise Linux](https://developers.redhat.com/products/rhel/overview/) landing page and click "Download" to get yourself a free, developers edition of RHEL. From there you can install it as a *.iso (ie, so you can import it into virtualbox or something)

If you're using the developers.redhat.com developer's copy of RHEL, you should automatically have a subscription to the right repos/channels to download and install Interconnect Router.

You should be able to:

```bash
sudo yum install qpid-proton-c python-qpid-proton
sudo yum install qpid-dispatch-router qpid-dispatch-tools
```

If you've got your own RHEL 7 instance or you cannot install based on the above commands, we'll try to manually install. The binaries can be given out during in-person workshops.

Once you've got RHEL set up, you can then install the Interconnect Router. First [Download a RHEL7 build of the Interconnect Router]() onto your RHEL machine (again, these binaries would be given out in person) 


Known to work on base RHEL7.x/Centos7.x distros:

```bash
# Enable EPEL
sudo rpm -i https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

# Yum update
sudo yum -y update

# Install preprequisites
sudo yum -y install qpid-proton-c-devel python-qpid-proton

# Install Official release of router
sudo rpm -Uvh libwebsockets-2.1.0-3.el7.x86_64.rpm
sudo rpm -Uvh qpid-dispatch-router-0.8.0-9.el7.x86_64.rpm 
```

### Option 2 Using Docker

With option 2, you can choose to use Docker and grab docker images. Note you'll need some kind of Linux machine (or VM) if your Host machine isn't Linux. This could be Docker for Mac/Windows, Minikube, or the Red Hat CDK 3.0. In any event, setting up Linux for Docker usage is beyond the scope of this installation documentation. Also, when doing any kind of port forwarding between the docker guest VM and your local workstation host, it's up to you to figure out how to open up the ports if you're trying to communicate locally with it. 
 
To get the docker image that contains the AMQ7 Interconnect Router: 

```bash
docker pull ceposta/qdr:latest
```

You can run the image like this:

```bash
docker run -itd --name qdr ceposta/qdr:latest
```

In the next labs, we'll see how to attach configuration to the router. Default configuration is in `/etc/qpid-dispatch/qdrouterd.conf`


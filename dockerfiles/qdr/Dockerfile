FROM centos:centos7.3.1611

RUN rpm -i https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
RUN yum -y update
RUN yum -y install qpid-proton-c-devel python-qpid-proton man

WORKDIR /opt/amq7

RUN mkdir -p /opt/amq7/router

COPY ./rpms ./router

RUN  rpm -Uvh ./router/*.rpm

VOLUME /etc/qpid-dispatch/

EXPOSE 5672 55672 5671
CMD ["/usr/sbin/qdrouterd", "--conf", "/etc/qpid-dispatch/qdrouterd.conf"]
FROM registry.access.redhat.com/rhel7/rhel

MAINTAINER Andrew Block <ablock@redhat.com>

LABEL io.k8s.description="Lightweight AMQP message router for building scalable, available, and performant messaging networks." \
      io.k8s.display-name="AMQ Interconnect Dispatch Router" \
      io.openshift.tags="amq,java,interconnect,router" \
      io.openshift.expose-services="5671,5001,5672,55672,10002,10003"


RUN yum repolist > /dev/null && \
    yum-config-manager --enable amq-interconnect-1-for-rhel-7-server-rpms --enable a-mq-clients-1-for-rhel-7-server-rpms && \
    yum clean all && \
    INSTALL_PKGS="qpid-proton-c \
    python-qpid-proton \
    qpid-dispatch-router \
    qpid-dispatch-tools" && \
    yum install -y --setopt=tsflags=nodocs install $INSTALL_PKGS && \
    rpm -V $INSTALL_PKGS && \
    yum clean all

EXPOSE 5001 5672 55672 10002 10003

CMD ["/usr/sbin/qdrouterd", "-c", "/etc/qpid-dispatch/qdrouterd.conf"]
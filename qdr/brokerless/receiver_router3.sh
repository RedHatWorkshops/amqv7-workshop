#!/bin/sh

docker run -e QPID_LOG_ENABLE=trace+ --link router3:router3 -t -i scholzj/qpid-cpp:latest qpid-receive -b router3:5672 --connection-options "{protocol: amqp1.0}" -a "'/myAddress'" -m 1 -f --print-headers yes
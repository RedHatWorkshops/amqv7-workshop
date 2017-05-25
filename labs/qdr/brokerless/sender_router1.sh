#!/bin/sh

docker run -e QPID_LOG_ENABLE=trace+ --link router1:router1 -t -i scholzj/qpid-cpp:latest qpid-send -b router1:5672 --connection-options "{protocol: amqp1.0}" -a "'/myAddress'" -m 1
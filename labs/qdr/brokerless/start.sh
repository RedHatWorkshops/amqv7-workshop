#!/bin/sh


docker run -itd --name router3 -v $(pwd)/router3.conf:/etc/qpid-dispatch/qdrouterd.conf ceposta/qdr
docker run -itd --name router2 --link router3 -v $(pwd)/router2.conf:/etc/qpid-dispatch/qdrouterd.conf ceposta/qdr
docker run -itd --name router1 --link router3 --link router2 -v $(pwd)/router1.conf:/etc/qpid-dispatch/qdrouterd.conf ceposta/qdr
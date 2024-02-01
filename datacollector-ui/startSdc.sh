#!/bin/sh
ulimit -n 65500
export SDC_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -Dsdc.static-web.dir=$(pwd)/target/dist"
../dist/target/streamsets-datacollector-3.22.2/streamsets-datacollector-3.22.2/bin/streamsets dc

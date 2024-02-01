#!/bin/bash
#
# Copyright 2017 StreamSets Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# This script is sourced when using the data collector initd scripts
# Refer to the initd/README file for details
#

# user that will run the data collector, it must exist in the system
#
export COR_USER=sdc

# group of the user that will run the data collector, it must exist in the system
#
export COR_GROUP=sdc

# directory where the data collector will store pipelines and their runtime information
#
export COR_DATA=${COR_DATA:-/var/lib/coraline}

# directory where the data collector will read pipeline resource files from
#
export COR_RESOURCES=/var/lib/coraline-resources

# directory where the data collector write its logs
#
export COR_LOG=/var/log/coraline

# directory where the data collector will read its configuration
#
export COR_CONF=${COR_CONF:-/etc/coraline}

# Includes the JARs in extra lib in the root classloader, this is required to support
# Snappy compression in Cassandra
#
export COR_ROOT_CLASSPATH=${COR_ROOT_CLASSPATH:-${COR_DIST}/root-lib/'*'}

# SDC upon start will verify that `ulimit -n` will return at least the following,
# otherwise SDC start will fail.
export COR_FILE_LIMIT="${COR_FILE_LIMIT:-32768}"

# JVM options for the data collector process
#
export COR_JAVA_OPTS="-Xmx1024m -Xms1024m -server -XX:-OmitStackTraceInFastThrow ${COR_JAVA_OPTS}"

# Indicate that MapR Username/Password security is enabled
#export COR_JAVA_OPTS="-Dmaprlogin.password.enabled=true ${COR_JAVA_OPTS}"

# Java 8 (JDK 1.8) specific options
# by default, use CMS garbage collector
export COR_JAVA8_OPTS=${COR_JAVA8_OPTS:-"-XX:+UseConcMarkSweepGC -XX:+UseParNewGC -Djdk.nio.maxCachedBufferSize=262144"}

# Java 11 (JDK 11) specific options
# Option UseConcMarkSweepGC was deprecated in version 9.0 and will likely be removed in a future release
export COR_JAVA11_OPTS=${COR_JAVA11_OPTS:-"-Djdk.nio.maxCachedBufferSize=262144"}

# Java 14 (JDK 14) specific options
# Option UseConcMarkSweepGC; support was removed in 14.0
export COR_JAVA14_OPTS=${COR_JAVA14_OPTS:-"-Djdk.nio.maxCachedBufferSize=262144"}

# Java 15 (JDK 15) specific options
export COR_JAVA15_OPTS=${COR_JAVA15_OPTS:-"-Djdk.nio.maxCachedBufferSize=262144"}

# Enables/disables the JVM security manager
#
export COR_SECURITY_MANAGER_ENABLED=${COR_SECURITY_MANAGER_ENABLED:-true}

# Produce heap dump when SDC will die on OutOfMemoryError (into
export COR_HEAPDUMP_ON_OOM=${COR_HEAPDUMP_ON_OOM:-true}

# Optional path for the heap dump file, default is $COR_LOG/cor_heapdump_${timestamp}.hprof
#export COR_HEAPDUMP_PATH=

# Enable GC logging automatically
export COR_GC_LOGGING=${COR_GC_LOGGING:-true}

# SDC supports only limited set of JDK vendors, all others must be explicitly enabled
export COR_ALLOW_UNSUPPORTED_JDK=${COR_ALLOW_UNSUPPORTED_JDK:-false}

# For Cluster yarn streaming mode in CDH.
export SPARK_KAFKA_VERSION=0.10

Kafka Connect FTP
=================

[![Build Status](https://travis-ci.org/Eneco/kafka-connect-ftp.svg?branch=master)](https://travis-ci.org/Eneco/kafka-connect-ftp)

Monitors files on an FTP server and feeds changes into Kafka.

Remote directories of interest are to be provided. On a specified interval, the list of files in the directories is refreshed. Files are downloaded when they were not known before, or when their timestamp or size are changed. Only files with a timestamp younger than the specified maximum age are considered. Hashes of the files are maintained and used to check for content changes. Changed files are then fed into Kafka, either as a whole (update) or only the appended part (tail), depending on the configuration. Optionally, file bodies can be transformed through a pluggable system prior to putting it into Kafka.

Data Types
----------

Each Kafka record represents a file, and has the following types.

-   The format of the keys is configurable through `ftp.keystyle=string|struct`. It can be a `string` with the file name, or a `FileInfo` structure with `name: string` and `offset: long`. The offset is always `0` for files that are updated as a whole, and hence only relevant for tailed files.
-   The values of the records contain the body of the file as `bytes`.

Setup
-----

### Properties

In addition to the general configuration for Kafka connectors (e.g. name, connector.class, etc.) the following options are available.

| name                        | data type | required | default      | description                                   |
|:----------------------------|:----------|:---------|:-------------|:----------------------------------------------|
| `ftp.address`               | string    | yes      | -            | host\[:port\] of the ftp server               |
| `ftp.user`                  | string    | yes      | -            | username                                      |
| `ftp.password`              | string    | yes      | -            | password                                      |
| `ftp.refresh`               | string    | yes      | -            | iso8601 duration the server is polled         |
| `ftp.file.maxage`           | string    | yes      | -            | iso8601 duration how old files can be         |
| `ftp.keystyle`              | string    | yes      | -            | `string` or `struct`, see above               |
| `ftp.monitor.tail`          | list      | no       | -            | comma separated list of path:destinationtopic |
| `ftp.monitor.update`        | list      | no       | -            | comma separated list of path:destinationtopic |
| `ftp.sourcerecordconverter` | string    | no       | No operation | Source Record converter class name, see below |

An example file is [here](./example.properties).

### Tailing Versus Update as a Whole

The following rules are used.

-   *Tailed* files are *only* allowed to grow. Bytes that have been appended to it since a last inspection are yielded. Preceding bytes are not allowed to change;
-   *Updated* files can grow, shrink and change anywhere. The entire contents are yielded.

Usage
-----

Build.

    mvn clean package

Put jar into `CLASSPATH`.

    export CLASSPATH=`realpath ./target/kafka-connect-ftp-0.1-jar-with-dependencies.jar` 

With `$CONFLUENT_HOME` pointing to the root of your Confluent Platform installation, start.

    $CONFLUENT_HOME/bin/connect-standalone $CONFLUENT_HOME/etc/schema-registry/connect-avro-standalone.properties your.specific.properties

Data Converters
---------------

Instead of dumping whole file bodies (and the danger of exceeding Kafka's `message.max.bytes`), one might want to give an interpretation to the data contained in the files before putting it into Kafka.
For example, if the files that are fetched from the FTP are comma-separated values (CSVs), one might prefer to have a stream of CSV records instead.
To allow to do so, the connector provides a pluggable conversion of `SourceRecords`.
Right before sending a `SourceRecord` to the Connect framework, it is run through an object that implements:

```scala
package com.eneco.trading.kafka.connect.ftp.source

trait SourceRecordConverter extends Configurable {
  def convert(in:SourceRecord) : java.util.List[SourceRecord]
}
```

(for the Java people, read: `interface` instead of `trait`).

The default object that is used is a pass-through converter, an instance of:

```scala
class NopSourceRecordConverter extends SourceRecordConverter{
  override def configure(props: util.Map[String, _]): Unit = {}
  override def convert(in: SourceRecord): util.List[SourceRecord] = Seq(in).asJava
}
```

To override it, create your own implementation of `SourceRecordConverter`, put the jar into your `$CLASSPATH` and instruct the connector to use it via the .properties:

```
ftp.sourcerecordconverter=your.name.space.YourConverter
```

Docker
---------------

To build project, run tests, build, and start Docker image use this command:
```bash
./gradlew clean shadowJar runDockerImage
```

This would run `/container_libs/start-kafka-and-ftp-connect.sh` script inside the container. This script basically waits for 20 seconds for Kafka to set up and starts Kafka Connect.

More information could be found [here](https://hub.docker.com/r/wurstmeister/kafka/).

NOTE: FTP server should be already availiable. For example, you can use this [image](https://hub.docker.com/r/stilliard/pure-ftpd/).

Docker configuration
---------------
All connector settings are at `/src/main/resources/`. You can specify Kafka and Zookeper hostnames at ./Dockerfile

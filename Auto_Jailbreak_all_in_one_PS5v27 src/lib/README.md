# Purpose

This directory is reserved to store Java classes specific to BD-J platform. These are:
- Personal Basis Profile 1.1.2 (JSR 217)
- Java TV API 1.1 (JSR 927)
- Globally Executable Multimedia Home Platform (GEM)
- BD-J API

The latter 3 will be downloaded from Internet as part of the build process and placed in this directory.

PBP 1.1 specification is a bit more tricky since it replaces core Java classes which are contained in `java.base` module.
Moreover, there is no known external source from which PBP 1.1 binary JAR can be downloaded.
Therefore, this repository provides binary JARs containing PBP 1.1 classes. These were compiled from source of a now defunct PhoneME project.
The PBP API is split into 3 different JARs so that these can be used with `--patch-module` argument of the JVM to replace built-in JDK classes of the same name.

# PhoneME compilation
The provided [Dockerfile](Dockerfile-pbp) can be used to produce `pbp*.jar` files. The command to execute is:
```shell
docker build -f Dockerfile-pbp -o . .
```

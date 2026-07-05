# PlayStation 5 Remote JAR Loader
This project uses vulnerabilities discovered in BD-J layer of PS5 firmware version 7.61 and earlier to deploy a loader that is able to listen to JAR files and execute their main class.
This makes it easy to burn the BD-R disc with the loader just once and then keep on running new versions of the experimental code.
This repository provides all the necessary setup needed to create both the loader BD-R disc filesystem and the JAR to send to the PS5.

## Quickstart
1. Download the JAR Loader ISO release.
2. Burn it to a BD-R(E) disc and run it from the PS5 "Media" tab.
3. Download one of the pre-compiled JARs or compile your own by reading the steps below.
4. Send the JAR to the JAR Loader using NetCat, or using the JAR file itself, if the machine has Java installed: `java -jar [jarfile].jar [ip] [host]`.

## Prerequisites
* JDK 11 (PS5 uses Java 11 runtime)
* Apache Maven
* IntelliJ IDEA Community Edition (optional, but recommended)

## Structure
The project comprises the following components:
* Root `pom.xml` defines the common properties and Maven plugin configuration for all the projects.
* `assembly` subproject creates the directory that should be burned to a BD-R disc. I recommend `ImgBurn` software to do this. Make sure to use the UDF 2.50 filesystem, then simply drag the contents of `assembly/target/assembly-[version]` directory to in the disc layout editor.
* `bdj-tools` subproject does not need to be touched. These are the utilities from HD Cookbook, adapted to run on JDK 11 and integrated into the build process of the BD-R disc filesystem.
* `stubs` subproject contains the build script to download BD-J class files from HD Cookbook and organize them for use with local JDK 11. It's also a place where PS5-specific stub files should be declared so that they can be used in the Xlet and the remote JAR.
* `sdk` subproject contains helper classes that simplify native invocation in the executed code. The classes in this module are embedded in the final JAR that will be sent to PS5 for execution.
* `xlet` subproject contains the code of the Xlet that starts when BD-R disc is launched on PS5. It simply starts the JAR loader (by default on port 9025).
* `xploit` subproject contains various payloads to be sent for execution on PS5. Each payload is a submodule of `xploit` module and produces its own JAR file. The code in the JAR can reference classes from `xlet`, such as the [Status](xlet/src/main/java/org/ps5jb/loader/Status.java) class to output on screen.
    * `jar` - Utility classes for interacting with the JAR Loader. It is not a payload per-se but is packaged in every payload JAR to handle the hand-off from the JAR loader to payload's `run` method.
    * `umtx/umtx1` - Implementation of UMTX exploit to obtain kernel read/write capabilities. Note that starting with firmware 6.00, kernel data segment is protected from writes.
    * `umtx/umtx2` - Alternative implementation of UMTX exploit.
    * `byepervisor` - Implementation of Byepervisor exploit, which enables bypassing of PS5 hypervisor to obtain kernel code read/write capabilities on firmware below 3.00.
    * `kerneldump` - In combination with UMTX and/or Byepervisor, this payload sends the dump of kernel over network.
    * `ftpserver` - Limited FTP server.
    * `samples` - Various trivial samples to demonstrate various capabilities of BD-J platform.

## Configuration
The following properties in [xlet/pom.xml](xlet/pom.xml) can be adjusted before compiling and burning the JAR Loader to disk:
* `loader.port` - Port on which JAR loader will listen for data.
* `loader.resolution.width`, `loader.resolution.height` - Screen resolution to set in various files. Not sure how this affects anything, I did not experiment with this enough.
* `loader.logger.host` - IP address where to echo the messages shown on screen. If blank, remote logging will not be used. This host can also receive binary data, see [RemoteLogger#sendBytes](xlet/src/main/java/org/ps5jb/loader/RemoteLogger.java).
* `loader.logger.port` - Port on which remote logger will send the status messages.
* `loader.logger.timeout` - Number of milliseconds to wait before abandoning attempts to connect to the remote logging host. If host is down after this timeout on the first send attempt, no further tries to do remote logging will be done.
* `loader.payload.root` - It's possible to include JAR payloads into the disc assembly (see below). This configuration parameter specifies the path relative to the disc root where the payloads will be placed.

Either modify the POM directly, or pass the new values from command line, example: `mvn clean package -Dloader.port=9025 -Dloader.logger.host=192.168.1.100`. To listen for messages on the remote machine when remote logger is activated, use `socat udp-recv:[remote.logger.port] stdout`.

Even if the remote logger is not active by default in the Xlet burned on disc, it is possible to change the remote server configuration using one of the two approaches:
1. Specify `xploit.logger.host` and optionally `xploit.logger.port` properties when compiling the JAR. These can be set in [xploit/pom.xml](xploit/pom.xml) or on command line `mvn clean package -Dxploit.logger.host=192.168.1.110`.
2. Programmatically in the JAR payload by calling [Status#resetLogger](xlet/src/main/java/org/ps5jb/loader/Status.java).

## Usage
1. Make sure environment variable `JAVA_HOME` points to the root of JDK 11. Add `${JAVA_HOME}/bin` directory to `${PATH}`.
2. Also make sure that `MAVEN_HOME` points to the root of Apache Maven installation. Add `${MAVEN_HOME}/bin` directory to `${PATH}`.
3. To create the payload follow these steps:
   * Make a copy of one of the sample payloads by copying the whole directory and placing it in [xploit](xploit) directory.
   * In `pom.xml` of the new payload, set `artifactId` of parent to "xploit", set `groupId` of the module to "org.ps5jb.xploit" and set `artifactId` of the module to the name of your payload.
   * Create a class implementing "Runnable" interface in the "org.ps5jb.client.payloads" package of the new module. The code inside "run" method will be the entry point of the payload.
   * Back in `pom.xml`, set the property `xploit.payload` to the name of the class above. If the class was created in a subpackage, then fully qualified name of the class is required. Otherwise, simly specify the name of the class without the package.
4. Execute `mvn clean package` from the root of the project. It should produce the following artifacts:
   * Directory `assembly/target/assembly-[version]` contains all the files that should be burned to a BD-R disc.
   * File `xploit/[payload]/target/[payload]-[version].jar` contains the code that can be sent repeatedly to the PS5 once the loader is deployed.
   It's possible to include the generated payload JAR into the disc assembly for loading from the menu rather than remotely. To do so, activate the profile `xploitOnDisc` while compiling, e.g. `mvn clean package -P xploitOnDisc`.
5. Burn the BD-R (better yet BD-RE) with the contents from the directory mentioned in the step 4a. Note that re-burning the JAR loader disc is only necessary when the source of [xlet](xlet) or [assembly](assembly) modules is changed or if the payload was included in the disc assembly in the previous step.
6. Insert the disc into the PS5 and launch "PS5 JAR Loader" from Media / Disc Player.
7. A message on screen should inform about loader waiting for JAR or the menu will be displayed if payloads are found on disc.
8. For remote execution, send the JAR using the command:
   ```shell
   java -jar xploit/[payload]/target/[payload]-[version].jar <ps5 ip address>`
   ```
   PS5 should inform on screen about the status of the upload and the execution.
9. Once remote execution is complete, the loader will wait for a new JAR. Do the necessary modifications in `xploit` project, recompile using `mvn package` and re-execute step 8 to retry as many times as necessary.

## Notes
1. To use with IntelliJ, point `File -> Open` dialog to the root of the project. Maven import will occur. Then follow manual steps in [IntelliJ Project Structure](#intellij-project-structure) to adjust the dependencies so that IntelliJ sees BD-J classes ahead of JDK classes.
2. If any of POMs are modified, it's necessary to do `Maven -> Reload Project` in IntelliJ to sync the project files.
3. To generate Javadocs, use `mvn verify` rather than `mvn package`. The Javadocs are enabled for [sdk](sdk), [xlet](xlet) and [xploit](xploit) modules and are generated in the `target/site/apidocs` directory of each module.
4. To run unit tests, use `mvn test`. Tests are run automatically on each compilation as well. To skip them, add `-DskipTests` property on command line. Note that not many unit tests are currently present since a lot of functionality is PS5 dependent.
5. If the `xploit` JAR does not have PS5 specific dependencies, it can be tested locally. The important part is to have `xlet`, `stubs` and `xploit` JARs all in the same folder. If the payload refers to GEM, BD-J or Java TV API, the corresponding JAR files generated in [lib](lib) directory should also be present in the same folder. Maven build automatically creates this arrangement in `target` directory of each payload so the command to run the payload on development machine is very similar to the one that sends the JAR to PS5:
   ```shell
   java -jar xploit/[payload]/target/[payload]-[version].jar
   ```
   When running locally, the `Status` class prints to standard output/error, rather than on `Screen`.
6. There are currently two separate version numbers in use by the project:
   * The `xlet` version is independent and will only be incremented when new disc needs to be burned with the updated JAR loader classes. If the PS5 shows a version different from the one produced by the code of this repo, payloads are not guaranteed to be compatible, so it's best to burn a new loader disc. This version is not expected to be incremented often as the loader is pretty stable. To increment this version, change the value of `xlet.version` property in [pom.xml](pom.xml).
   * The rest of the modules use the version from the parent POM. This version will be incremented with the new release and reflects that either the SDK or the payloads have changed. If the loader version remained the same, these new versions of payloads can still be sent to the JAR loader without re-burning the disc. This version can be incremented by executing `mvn versions:set -DnewVersion=[version]`, then refreshing the IntelliJ Maven project as described in bullet point number 2.

## IntelliJ Project Structure
IntelliJ Maven project files are located in a private local folder of IntelliJ. Initial opening and the following reloads of the Maven project incorrectly import some of the settings. In particular, BD-J stack JARs are completely ignored or are imported with a wrong scope. Unfortunately, due to this fact, the following steps need to be performed every time a Maven project reload occurs:
* Syncing Maven project modifies [.idea/compiler.xml](.idea/compiler.xml) to contain absolute system paths. Simply replace those with `$PROJECT_DIR$` macro again.
* Go to `Project Structure` window and switch to `Modules` tab. Go through every module and make sure that the modules `bdj-api`, `javatv-api` and `gem-api` have "Provided" scope.
* In addition, for all the modules that have the above-mentioned dependencies, click on `+ (Add) -> Library` button and add `bdjstack` library dependency. Make sure it is moved in the top position above SDK 11 entry. This setting used to be commited to version control and could be simply reverted, but in recent updates, it has to be performed every time. 

## Credits
There are so many who decided to share the knowledge with the community to make this project possible.
- [Andy "theflow" Nguyen](https://github.com/theofficialflow) for discovering and sharing BD-J vulnerabilities and native execution techniques without which none of the work in this repo would be possible.
- Specter for his Webkit implementations of PS5 kernel access which served as a base for Java implementation: [IPV6](https://github.com/Cryptogenic/PS5-IPV6-Kernel-Exploit), [UMTX](https://github.com/PS5Dev/PS5-UMTX-Jailbreak/) and [Byepervisor](https://github.com/PS5Dev/Byepervisor).
- [Flat_z](https://github.com/flatz) for pretty much everything of significance that happened in PlayStation scene since as far back as PS3, including the UMTX exploitation strategy and AGC-based kernel r/w contained in this repo.
- [Cheburek3000](https://github.com/cheburek3000) for contributing an alternative implementation of UMTX exploitation. 
- [bigboss](https://github.com/psxdev) and [John Törnblom](https://github.com/john-tornblom) for their work specifically in BD-J area.
- [iakdev](https://github.com/iakdev) for the contribution of the menu loader.
- CryoNumb for ideas, code contributions and helping with testing on various firmware versions.
- All the other contributors to Specter's Webkit implementations: [ChendoChap](https://github.com/ChendoChap), [Znullptr](https://twitter.com/Znullptr), [sleirsgoevy](https://twitter.com/sleirsgoevy), [zecoxao](https://twitter.com/notnotzecoxao), [SocracticBliss](https://twitter.com/SocraticBliss), SlidyBat, [idlesauce](https://github.com/idlesauce), [fail0verflow](https://fail0verflow.com/blog/) [kiwidog](https://kiwidog.me/), [EchoStretch](https://github.com/EchoStretch), [LM](https://github.com/LightningMods).
- Testers of various firmware revisions: jamdoogen, Twan322, MSZ_MGS, KingMaxLeo, RdSklld, Ishaan, Kirua, PLK, benja44_, MisaAmane, Unai G, Leo.

Sample BD-J payloads in this repository are adaptations of the following work:
- FTP server by [pReya](https://github.com/pReya/ftpServer). 
- Mini Tennis by [Edu4Java](http://www.edu4java.com/en/game/game0-en.html).

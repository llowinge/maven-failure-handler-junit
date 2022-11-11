# Maven failure handler junit extension
## Description of problem
When running JUnit test it can happen that the test is not run because of some previous maven phase failure (eg. dependency cannot be downloaded, compilation error, etc..). We could use some tool (eg. Jenkins JUnit plugin) to visualize test results, where we don't have to rely on reading the log. In that case we wouldn't see the failure on the first look and we could potentionally overlook it.

## What the extension does
It will catch failure occurred during any maven phase which was executed before test (surefire/failsafe) phase. From those failures are then constructed artificial XML surefire report files and placed under `<module>/target/surefire-reports`. 

## Build and use
Firstly build the extension.

    mvn clean install
Then use it in your test run.

    -Dmaven.ext.class.path=<jar-file>

Or placing XML into `${maven.multiModuleProjectDirectory}/.mvn/extensions.xml`:
```
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
  <extension>
    <groupId>org.jboss.fuse.maven</groupId>
    <artifactId>maven-failure-handler-junit</artifactId>
    <version>1.0-SNAPSHOT</version>
  </extension>
</extensions>
```

## Articles and other sources
* https://stackoverflow.com/questions/41893919/run-a-maven-plugin-when-the-build-fails
* https://maven.apache.org/studies/extension-demo/
* http://takari.io/2015/03/19/core-extensions.html

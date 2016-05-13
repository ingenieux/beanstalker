# Developer Notes

Personal notes since I often switch machines, configurations and lose my mind

## Integration Testing

```$ mvn clean install verify -Prelease,it```

  * But make sure your settings.xml contains sensible defaults. Those are mine:

```
    <beanstalk.keyName>aldrin@leal.eng.br</beanstalk.keyName>
    <beanstalk.iamInstanceProfile>aws-elasticbeanstalk-ec2-role</beanstalk.iamInstanceProfile>
    <beanstalk.instanceType>m1.small</beanstalk.instanceType>
```

  * Use this to force cleanup (in case it fails - post-integration test should deal with it anyway):

```$ mvn -f beanstalk-maven-plugin-it/pom.xml compile exec:java -Dexec.mainClass=br.com.ingenieux.beanstalker.it.ProjectCleaner```

  * You can also override the default app name, via the beanstalk.project.name variable:

```$ mvn clean install verify -Prelease,it -Dbeanstalk.project.name=mbit-something```

**Please base it on mbit-whatever, as it is used as a hint by ProjectCleaner to figure out what to cleanup**

## git

```
$ git config --global core.autocrlf input
```

## Code Style

Google. Please set up your IDE accordingly prior to submitting pull requests.

## Release Process

The classical:

```
$ mvn release:prepare release:perform -DautoVersionSubmodules=true
```

When something fails:

```
$ mvn release:rollback # (or release:clean)
$ git tag -d <tagname> && git push origin :refs/tags/<tagname>
```
## Idea Setup (copyright)

  * Scope Exp: ```!file:target//*&&!file:src/main/resources/archetype-resources//```
  * Keyword to detect copyright in comments: "Licensed"
  * Copyright Text:

```
Copyright (c) $today.year ingenieux Labs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Building on Windows?

 * [Oh crap, its' mvn.cmd](https://issues.apache.org/jira/browse/ARCHETYPE-488?focusedCommentId=14730954&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14730954)

## TODO

  * Add ELB Tweaks / Support (healthchecks / other options)
  * Introduce a Docker archetype
  * Fix my typos in descriptions
  * Allow more flexible support for other platforms (e.g., for Docker, switch the namespace and option settings as such)
  * Consider using an hybrid platform so gradle/others could rely upon
    * B plan: Using @RequiresDirectInvocation Mojos
  * Coverage Testing / SonarQube
  * clone-environment
  * Upgrade to Java 8 to leverage Lambdas
  * Employ better parameter validation
  * debug-environment:
    * https://github.com/aws/aws-toolkit-eclipse/blob/7641a135dbb0571e40aff32f81e11dbf34366431/com.amazonaws.eclipse.elasticbeanstalk/src/com/amazonaws/eclipse/elasticbeanstalk/EnvironmentBehavior.java
    * https://github.com/aws/aws-toolkit-eclipse/blob/911c2c3402b357ed94857e06c1f4fbe0b040b930/com.amazonaws.eclipse.elasticbeanstalk/src/com/amazonaws/eclipse/elasticbeanstalk/Environment.java
    * https://github.com/aws/aws-toolkit-eclipse/blob/911c2c3402b357ed94857e06c1f4fbe0b040b930/com.amazonaws.eclipse.elasticbeanstalk/src/com/amazonaws/eclipse/elasticbeanstalk/jobs/UpdateEnvironmentJob.java
    *
    https://github.com/aws/aws-toolkit-eclipse/blob/7641a135dbb0571e40aff32f81e11dbf34366431/com.amazonaws.eclipse.ec2/src/com/amazonaws/ec2/cluster/Cluster.java
  * Optimize the build - site/site-deploy seems too slow
  * Update docs site


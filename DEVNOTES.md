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

## Git / HG

We keep the git mirror mostly as a courtesy to our users. We base ours on hg instead.

  * Installation needs .hgrc correctly ([hg-git](http://hg-git.github.io/) is needed).
```
[ui]
username = John Doe <john@doe.org>

[extensions]
hgext.git =
hgext.bookmarks =
```
  * On a fresh checkout, github alias must be set:

```
$ cat .hg/hgrc
[paths]
default = ssh://hg@bitbucket.org/aldrinleal/beanstalker
github = git+ssh://git@github.com/ingenieux/beanstalker.git
```

  * ```hg pull -u && hg push github && mvn clean deploy -Prelease && mvn site site-deploy -Prelease```

## Release Process

The classical:

```$ mvn release:prepare release:perform -DautoVersionSubmodules=true```

## TODO

  * Coverage Testing / SonarQube
  * clone-environment
  * debug-environment
  * Optimize the build - site/site-deploy seems too slow
  * Update docs site
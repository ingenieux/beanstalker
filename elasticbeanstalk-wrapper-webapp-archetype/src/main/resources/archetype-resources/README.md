# Welcome to your beanstalk-maven-plugin wrapper archetype project!

## About

For up to date information, see http://docs.ingenieux.com.br/project/beanstalker/

This project was generated from the elasticbeanstalk-wrapper-webapp-archetype. 

As it is, it is a boilerplate code for wrapping an existing .war file and deploying into Amazon Web Services' Elastic Beanstalk Service.

We hope you like it. If you run into any problems, please let us know by the [mailing list](http://groups.google.com/group/beanstalker-users) or the [issue tracker](http://github.com/ingenieux/beanstalker/issues) 

## Setting up your Maven Build

TL;DR: Set AWS_ACCESS_KEY and AWS_SECRET_KEY from your environment.

Create / Edit your settings.xml as suggested in the [Security Page](http://beanstalker.ingenieux.com.br/beanstalk-maven-plugin/security.html) 

This project, as is, supports modes 1-2 (settings.xml), and it looks by default for aws.amazon.com. 

*Don't worry, you're likely to do it once for each and every machine you plan to build. :)*

## Configuring your Project

We suggest convention over configuration, but anyway. The only setting you're likely to set is your ```cnamePrefix``` and ```environmentRef```` properties on your pom.xml file. 

Make sure you pick something unique and unlikely to conflict with other users.

It is also interesting to review the parameters in the ```properties``` section.

## Deploying

Simply call ```mvn deploy -Pfast-deploy```

If your environment is Ready (Green or Red), it will deploy. If not, a new version will get published. 

*You can always launch a new environment...*

## Launching an Environment

If you did the previous step, it will create a new elastic beanstalk application for you in AWS Console. You can launch and environment right away with:

```mvn beanstalk:create-environment```

Or you could use the console. From the console, click in "Launch New Environment". In this dialog, not all options are available, so initially set the ```Health Check URL``` to the /debug handler:

```/services/api/v1/debug```

Don't launch it yet, though. We have some tips...

## Deploy versus Fast-Deploy

There are two variables involved when deploying into AWS Elastic Beanstalk:

  - Git versus S3 (we chose git in this project)
  - Downtime or Zero-Downtime

Since we already made your life simpler by choosing git over S3, you have two profiles to pick according to your needs:

  - Production: mvn -Pdeploy deploy will do a zero-downtime deploy. No downtime, but uses more resources
  - Development: mvn -Pfast-deploy deploy will do a plain git deploy. It will incur downtime, but uses less resources

## Environment / Configuration Tips:

  - We suggest you save your environment into a configuration template once you're happy. We suggest ```envname-yyyymmdd-nn```, where NN is a number which gets incremented. 

Hint: Use ```mvn beanstalk:tag-environment``` to do this. If you want to launch a new environment based on this template, simply use ```mvn beanstalk:create-environment -Dbeanstalk.templateName=envname-yyyymmdd-nn```

## Setting the Proper Health Check URL

Besides that, make sure you can map to an SSH Key you already have, so you are able to log into your EC2 Instance and troubleshoot any problems (unlikely, but better safe than sorry).

Ok, now you can launch.

Once the application is launched (and there's a Green Icon), click in "Edit Configuration" and set your applications AWS Access Key / Shared Key, then you can set set the proper Health Check URL:

```/services/api/v1/health/check```

## SCM Notes

### Git Fast-Deploy

Please never commit the contents of your ```tmp-git-staging``` directory. 

Its there to cache locally and enable fast deployments into elastic beanstalk via the git backend.

(if you look closely, thats the reason for both .gitignore and .hgignore files)

## Misc Notes

 - Check [the plugin page](http://beanstalker.ingenieux.com.br/beanstalk-maven-plugin/) for General Reference and Usage Instructions.
 - Subscribe to the beanstalker-users list at [[http://groups.google.com/group/beanstalker-users]] to get up-to-date information
 - Problems? Let us know, on the lists or [the issue tracker](http://github.com/ingenieux/beanstalker/issues)
 - Feedback please!

We hope beanstalk-maven-plugin helps your life easier, and find it fun and easy as much as we do.

btw, why not [donate to beanstalker](http://beanstalker.ingenieux.com.br/donate.html)?

Thank you.

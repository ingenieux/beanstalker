# Welcome to your beanstalk-webapp Archetype Project

## About

This project was generated from the elasticbeanstalk-service-webapp-archetype. 

As it is, it is a boilerplate code for a generic, modern webapp using Amazon Web Services' Elastic Beanstalk Service.

It also includes some tips and tricks we've learned over the past years when using Elastic Beanstalk. They include:

  - Custom Fixtures for Embedded in-Container Testing (see ServerRule class)
  - Another one for [Guice](http://code.google.com/p/google-guice/)-based startup (ContainerRule)
  - Uses [rest-assured](http://code.google.com/p/rest-assured/) for fluent JAX-RS testing
  - ```logback.xml``` (and ```logback-test.xml```) files, including Syslog usage for [PaperTrail](https://papertrailapp.com/?thank=cffa7e)
  - One single [Guice](http://code.google.com/p/google-guice/) module for each and every AWS-supported Java Client (BaseAWSModule)
  - Support for dealing with the configuration parameters for Beanstalk (BeanstalkCredentialsProviderChain)
  - [Jackson](http://wiki.fasterxml.com/JacksonHome), [Jersey](http://jersey.java.net/), and [Guice](http://code.google.com/p/google-guice/) configured out of the box (although you're likely to run into problems if you ever need viewables)
  - Basic JAX-RS Boilerplate Code
  - A [Simple Notification Service (SNS)](http://aws.amazon.com/sns/) Notification JAX-RS Resource on ```BaseSNSResource``` and ```SNSResource``` (useful if you need to plug-in notifications)
  - A Basic Health Code Check (handy!)

In fact, this archetype was derived from an internal ingenieux project (a backend for [ekaterminal](http://www.ingenieux.com.br/products/ekaterminal/), 
which is another piece of software we are fairly confident you'll love it if you do elastic beanstalk/mapreduce too often.

We hope you like it. If you run into any problems, please let us know by the [mailing list](http://groups.google.com/group/beanstalker-users) or the [issue tracker](http://github.com/ingenieux/beanstalker/issues) 

## Setting up your Maven Build

Create / Edit your settings.xml as suggested in the [Security Page](http://beanstalker.ingenieux.com.br/beanstalk-maven-plugin/security.html) 

This project, as is, supports modes 1-2 (settings.xml), and it looks by default for aws.amazon.com. 

*Don't worry, you're likely to do it once for each and every machine you plan to build. :)*

## Deploying

Simply call ```mvn deploy -Pdeploy```

## SCM Notes

### Git Fast-Deploy

Please never commit the contents of your ```tmp-git-staging``` directory. 

Its there to cache locally and enable fast deployments into elastic beanstalk via the git backend.

(if you looked, thats the reason for both .gitignore and .hgignore files)

# Code Notes

## Testing

The project includes a lot of boilerplate code useful for AWS. We tried to achieve a mix (e.g., avoiding adding too much dependencies), and if you want, you're free to get rid of it.

## Health Check

The health check code tries to poll DynamoDB, S3, and EC2. If your EC2 keys are IAM-limited, comment-out the relevant sections.

## JAX-RS

This project has a few particular details. If you want to rename your root resource path, look under ```WebModule``` instead of ```web.xml```.

## Misc Notes

 - Check [the plugin page](http://beanstalker.ingenieux.com.br/beanstalk-maven-plugin/) for General Reference and Usage Instructions.
 - Subscribe to the beanstalker-users list at [[http://groups.google.com/group/beanstalker-users]] to get up-to-date information
 - Problems? Let us know, on the lists or [the issue tracker](http://github.com/ingenieux/beanstalker/issues)
 - I <3 feedback!

We hope beanstalk-maven-plugin made your life easier, and find it fun and easy as much as we do.

btw, why not [donate to beanstalker](http://beanstalker.ingenieux.com.br/donate.html)?

Thank you.

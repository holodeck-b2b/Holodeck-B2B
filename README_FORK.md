# Why did we fork this repo?
We have had the problem that 
we could not detect why a p-mode 
configuration was not matching the 
message that were sent to us from 
eudamed.

After some trial and error the only
reasonable path forward was to extend 
the logging in the holodeckb2b application.

Other scenarios where we want to add logging
quickly might exist in the future.

# How to build the holockeckb2b from sources?

```cd Holodeck-B2B```

and then:

```mvn package```

This should create 

```
holodeck-resource/holockeckb2b-source/Holodeck-B2B/modules/holodeckb2b-distribution/target/holodeckb2b-distribution-6.1.0.zip
```
This zip can then be used in the Makefile.
If you want the standard installation use 
the packaged zip file provided by the 
holodeck website
http://holodeck-b2b.org/download/
or the repo: 
https://github.com/holodeck-b2b/Holodeck-B2B/tags.


## known pitfalls:

### missing dependencies:
We were able to install missing dependencies from the packaged
zip file of the version we wanted to build.

For example:
```
mvn install:install-file -DgroupId=org.holodeckb2b.extensions -DartifactId=file-backend -Dversion=1.3.0 -Dpackaging=jar -Dfile=./holodeckb2b-6.1.0/lib/file-backend-1.3.0.jar      
```
where ```./holodeckb2b-6.1.0/lib/file-backend-1.3.0.jar``` comes from 
the prepackaged holodeck distribution ```holodeckb2b-distribution-6.1.0.zip```
that can be downloaded on the holodeckb2b website: http://holodeck-b2b.org/download/
if it is the latest version or from the repo: https://github.com/holodeck-b2b/Holodeck-B2B/tags.

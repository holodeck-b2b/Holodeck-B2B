# Holodeck B2B
Holodeck B2B is a standalone system-to-system messaging solution that supports the OASIS [ebMS3 Messaging](http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/ebms_core-3.0-spec.html) and the [AS4 Profile](http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/AS4-profile-v1.0.html) Standards. Being Java based, it will run on most platforms.

It is designed with extensibility in mind providing an interface layer (see the [interfaces project](interfaces)) which you can use to add or replace default implemented functionality. 

__________________  
For more information on using Holodeck B2B visit the website at https://holodeck-b2b.org    
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/Holodeck-B2B  
Issue tracker https://github.com/holodeck-b2b/Holodeck-B2B/issues

## Installation
### Prerequisites
The only requirement to run Holodeck B2B is that you have installed a Java run-time environment version 11 and that the `JAVA_HOME` environment variable is set to the path where the JRE is installed.  
If you have multiple Java run-times installed on your system, it is recommended to set the `JAVA_HOME` variable inside the Holodeck B2B start script (`setenv.sh` on Linux/MacOS or `startServer.bat` on Windows).

### Getting started guide
To help you execute your first message exchange using Holodeck B2B, you will find a [step-by-step guide to setting up Holodeck B2B](http://holodeck-b2b.org/documentation/getting-started/) on the project website.

## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code, including comments 
* commit incrementally with readable and detailed commit messages
* run tests to check everything works on runtime
* update the changelog with a short description of the changes including a reference to the issues fixed
* submit a pull request _against the `next` branch_ of this repository

If your contribution is more than a patch, please contact us beforehand to discuss which branch you can best submit the pull request to.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/Holodeck-B2B/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## License
The Holodeck B2B Core components and default implementations are licensed under the General Public License V3 (GPLv3). This means you are not allowed to include the provided Holodeck B2B components in a closed source **application**. You can however use the Holodeck B2B application **together with your closed source application** as long as you only use the provided file based integration method or interfaces (API's) to create your own integration to communicate with the Holodeck B2B core. For this purpose, the [interfaces project](https://github.com/holodeck-b2b/Holodeck-B2B/interfaces) is licensed under the Lesser General Public License V3 (LGPLv3).`

To implement the cryptographic algorithms Holodeck B2B uses the Bouncy Castle library provided by [The Legion of the Bouncy Castle Inc.](http://www.bouncycastle.org), see the `bc_license.txt` file.

## Support
Commercial Holodeck B2B support is provided by Chasquis. Visit [Chasquis-consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.

# Holodeck B2B
Holodeck B2B is a standalone B2B messaging solution. This project includes support for the OASIS specifications for ebMS3 and the AS4 profile. Java based, it will run on most platforms.

It is designed with extensibility in mind providing an interface layer (API) which you can find in the Interfaces module, and lots of documentation inside the code.

__________________
For more information on using Holodeck B2B visit the website at http://holodeck-b2b.org  
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/Holodeck-B2B  
Issue tracker https://github.com/holodeck-b2b/Holodeck-B2B/issues

## Installation
### Prerequisites
Java 8 or higher is required to run Holodeck B2B.

### Getting started guide
To help you set up your first instance of Holodeck B2B, you will find a [step-by-step guide to setting up Holodeck B2B](http://holodeck-b2b.org/documentation/getting-started/) on the project website.

## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code, including comments 
* commit incrementally with readable and detailed commit messages
* run integration tests to check everything works on runtime
* Update the changelog with a short description of the changes including a reference to the issues fixed
* submit a pull request *against the 'next' branch* of this repository

If your contribution is more than a patch, please contact us beforehand to discuss which branch you can best submit the pull request to.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/Holodeck-B2B/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## License
The Holodeck B2B core is licensed under the General Public License V3 (GPLv3) which is included in the license.txt in the root of the project.
This means you are not allowed to integrate Holodeck B2B in a closed source product. You can however use Holodeck B2B together with your closed source product as long as you only use the provided interfaces (API's) to communicate with the Holodeck B2B core.
For this purpose, the interfaces module is licensed under the Lesser General Public License V3 (LGPLv3).

To implement the cryptographic algorithms Holodeck B2B uses the Bouncy Castle library provided by [The Legion of the Bouncy Castle Inc.](http://www.bouncycastle.org), see the bc_license.txt file.

## Support
Commercial Holodeck B2B support is provided by Chasquis. Visit [Chasquis-consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.

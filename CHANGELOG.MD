# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.0.2
### Added
* Include derived _refToMessageId_ in Error signal when notifying business application about the error [#12](https://github.com/holodeck-b2b/Holodeck-B2B/issues/12)

### Fixed 
* PartyId can not be set on submission when security is used [#13](https://github.com/holodeck-b2b/Holodeck-B2B/issues/13)
* Long error descriptions can not be stored in the database [#14](https://github.com/holodeck-b2b/Holodeck-B2B/issues/14)

## 2.0.1
### Added
* Identification of Holodeck B2B in _User-Agent_ and _Server_ HTTP headers

### Fixed 
* Database doesn't save long meta data strings [#8](https://github.com/holodeck-b2b/Holodeck-B2B/issues/8)
* NPE when using PullRequestFlow without security configuration [#9](https://github.com/holodeck-b2b/Holodeck-B2B/issues/9)  
* IOException possible on parallel submission to Holodeck B2B Core [#10](https://github.com/holodeck-b2b/Holodeck-B2B/issues/10)

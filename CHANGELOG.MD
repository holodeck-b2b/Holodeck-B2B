# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.1.0 [Unreleased]
### Added
* Support for the AS4 Multi-hop feature as specified in [section 4 of the AS4 OASIS Standard](http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__21622_149522555).
**NOTE:** The addition for multi-hop support adds the `USES_MULTI_HOP` attribute to the `MessageUnit` persistency class 
and therefore also requires a database change for the corresponding `MSG_UNIT` table. 
* Event framework to enable additional notification to external components about events that occur during message 
processing. See package org.holodeckb2b.interface.events for the new interfaces. 
* Event for creation of a Receipt (see `org.holodeckb2b.interfaces.events.types.IReceiptCreatedEvent`)
* A _"trust store"_ to hold the certificates of trusted Certificate Authorities that are used to validate the certificates
used for signing a message. This _trust store_ should be used for certificates not directly related to a trading partner
* Possibility to configure the _Pull Worker Pool_ from outside the Holodeck B2B Core through the new 
  `HolodeckB2BCoreInterface.setPullWorkerPoolConfiguration` method 

### Changed
* The addition of the event framework resulted in changes to the Holodeck B2B Core and P-Modes interfaces to get access 
    to the _event processor_ and enable configuration of _event handlers_.
* The XML P-Mode implementation was changed to add the event handler configuration, see the new version of the schema in 
    **pmode.xsd**
* Pull Requests are now also _submitted_ to the Core and sent using the same _Sender worker_ as other message units. This 
  required a change in the `IMessageSubmitter` interface that now also accepts `IPullRequest` objects for submission.
  Together with the new ability to configure the _pull worker pool_ it allows extensions to set-up a custom mechanism 
  for triggering the pulling.

### Deprecated 

### Removed 
* The bundling of Signal Messages is disabled because it can cause problems in multi-hop exchanges and it is also not
widely interop tested. Disabling the bundling is done by removing the handlers that add the signals to the message
from the processing flow in the configuration of the Holodeck B2B module. Bundling will be enabled again in a future 
version.

### Fixed 

### Security

## 2.0.1
### Added
* Identification of Holodeck B2B in _User-Agent_ and _Server_ HTTP headers

### Fixed 
* Database doesn't save long meta data strings [#8](https://github.com/holodeck-b2b/Holodeck-B2B/issues/8)
* NPE when using PullRequestFlow without security configuration [#9](https://github.com/holodeck-b2b/Holodeck-B2B/issues/9)  
* IOException possible on parallel submission to Holodeck B2B Core [#10](https://github.com/holodeck-b2b/Holodeck-B2B/issues/10)


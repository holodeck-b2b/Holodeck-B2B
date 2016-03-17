# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
* Support for the AS4 Multi-hop feature as specified in [section 4 of the AS4 OASIS Standard](http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__21622_149522555).
**NOTE:** The addition for multi-hop support adds the `USES_MULTI_HOP` attribute to the `MessageUnit` persistency class 
and therefore also requires a database change for the corresponding `MSG_UNIT` table. 
* Event framework to enable additional notification to external components about events that occur during message 
processing. See package org.holodeckb2b.interface.events for the new interfaces. 
* Event for creation of a Receipt (see `org.holodeckb2b.interfaces.events.types.IReceiptCreatedEvent`)

### Changed
* The addition of the event framework resulted in changes to the Holodeck B2B Core and P-Modes interfaces to get access 
    to the _event processor_ and enable configuration of _event handlers_.
* The XML P-Mode implementation was changed to add the event handler configuration, see the new version of the schema in 
    **pmode.xsd**

### Deprecated 

### Removed 
* The bundling of Signal Messages is disabled because it can cause problems in multi-hop exchanges and it is also not
widely interop tested. Disabling the bundling is done by removing the handlers that add the signals to the message
from the processing flow in the configuration of the Holodeck B2B module. Bundling will be enabled again in a future 
version.

### Fixed 

### Security

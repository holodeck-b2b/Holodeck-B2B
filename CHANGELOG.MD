# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
* Event framework to enable additional notification to external components about events that occur during message 
    processing. See package org.holodeckb2b.interface.events for the new interfaces. 
* Event for creation of a Receipt (see org.holodeckb2b.interfaces.events.types.IReceiptCreatedEvent

### Changed
* The addition of the event framework resulted in changes to the Holodeck B2B Core and P-Modes interfaces to get access 
    to the _event processor_ and enable configuration of _event handlers_.
* The XML P-Mode implementation was changed to add the event handler configuration, see the new version of the schema in 
    **pmode.xsd**

### Deprecated 

### Removed 

### Fixed 

### Security

# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased
###Added
* P-Mode validation feature which separates the validation of P-Modes from both their internal and external storage
* Unit tests to improve test coverage
* Integration tests. These tests setup two Holodeck B2B instances and execute both a push and pull exchange to check
  that everything works on run time
* A default implementation of the message model interface (in Common module)
* Interfaces for separation of Core functionality and storage of message unit meta-data
* Default _persistency provider_ implementation

###Changed
* Refactored the validation of received message units into separate validation classes and handler
* Refactored Core module to use new persistency interfaces
* Moved XML message meta-data implementation and ProductId classes to Common module

###Deprecated

###Removed
* Exceptions `org.holodeckb2b.common.exceptions.DatabaseException` and `org.holodeckb2b.common.exceptions.DuplicateMessageIdError`
  which were bound to the specific persistency implementation of previous versions

###Fixed

###Security

## 2.1.2
###### 2016-12-21
###Fixed
* References in Error Signals are not checked correctly [#38](https://github.com/holodeck-b2b/Holodeck-B2B/issues/38)
* Database connection leak [#39](https://github.com/holodeck-b2b/Holodeck-B2B/issues/39)
* The dependency on Java 8 is removed again [#40](https://github.com/holodeck-b2b/Holodeck-B2B/issues/40)

## 2.1.1
###### 2016-11-11
###Changed
* It is now possible to not only configure whether _Error Signals_ should be logged but also if all signals
  should be logged or only the ones that contain an Error with severity _failure_ (see `log4j2.xml` for more info)

###Fixed
* Database connection not closed [#34](https://github.com/holodeck-b2b/Holodeck-B2B/issues/34)
* ebMS errors are only logged when log level is ERROR [#35](https://github.com/holodeck-b2b/Holodeck-B2B/issues/35)
* Exceptions in processing of WSS header not handled correctly [#36](https://github.com/holodeck-b2b/Holodeck-B2B/issues/36)

## 2.1.0
###### 2016-10-18
### Added
* Support for the AS4 Multi-hop feature as specified in [section 4 of the AS4 OASIS Standard](http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__21622_149522555).
* Event framework to enable additional notification to external components about events that occur during message
processing. See `org.holodeckb2b.interface.events` for the new interfaces.
* Default implementation of message purging. This default implementation will remove all meta-data (including payload
  data for User Messages) after a configurable number of days has passed since the last change to the message unit's
  processing state.
* Events for creation of a Receipt (see `org.holodeckb2b.interfaces.events.types.IReceiptCreatedEvent`), creation of
 a signature for a User Message (see `org.holodeckb2b.interfaces.events.types.ISignatureCreatedEvent`) and for the
 removal of an "expired" User Message (see `org.holodeckb2b.interfaces.events.types.IMessageUnitPurgedEvent`).
* A _trust store_ to hold the certificates of trusted Certificate Authorities that are used to validate the certificates
used for signing a message. This _trust store_ should be used for certificates not directly related to a trading partner.
* Possibility to configure the _Pull Worker Pool_ from outside the Holodeck B2B Core through the new
  `HolodeckB2BCoreInterface.setPullWorkerPoolConfiguration` method.
* When a received Error signal does not reference a sent message unit a _ValueInconsistent_ error is generated. Note that
  this can be caused by either an invalid or missing reference in the Error signal.

### Changed
* The addition of the event framework resulted in changes to the Holodeck B2B Core and P-Modes interfaces to get access
    to the _event processor_ and enable configuration of _event handlers_.
* The XML P-Mode implementation was changed to add the event handler configuration, see the new version of the schema in
    **pmode.xsd**.
* Pull Requests are now also _submitted_ to the Core. This required a change in the `IMessageSubmitter` interface that
    now also accepts `IPullRequest` objects for submission. Together with the new ability to configure the
    _pull worker pool_ it allows extensions to set-up a custom mechanism for triggering the pulling.
* Received ebMS Error signals are now logged to the special error log `org.holodeckb2b.msgproc.errors.received` regardless
   whether they can be processed completely or need to be notified to the _Producer_ application.
* Messages without attachments are now sent as single part messages, i.e. not using the SOAP with Attachments feature.

### Removed
* The bundling of Signal Messages is disabled because it can cause problems in multi-hop exchanges and it is also not
widely interop tested. Disabling the bundling is done by removing the handlers that add the signals to the message
from the processing flow in the configuration of the Holodeck B2B module. Bundling will be enabled again in a future
version.

### Fixed
* PathWatcher does not honour system home directory [#19](https://github.com/holodeck-b2b/Holodeck-B2B/issues/19). Thanks @[phax](https://github.com/phax).
* Problem with renaming mmd file to result extension if a file with the same name already exists.
* Payload with MIME type "application/gzip" is not decompressed [#24](https://github.com/holodeck-b2b/Holodeck-B2B/issues/24).
* Rejected submission when multiple PartyIds were specified in both submission and P-Mode.

### Thank you
[Philip Helger](https://github.com/phax) for various general code improvements.

## 2.0.2
###### 2016-05-17
### Added
* Include derived _refToMessageId_ in Error signal when notifying business application about the error [#12](https://github.com/holodeck-b2b/Holodeck-B2B/issues/12).

### Fixed
* PartyId can not be set on submission when security is used [#13](https://github.com/holodeck-b2b/Holodeck-B2B/issues/13)
* Long error descriptions can not be stored in the database [#14](https://github.com/holodeck-b2b/Holodeck-B2B/issues/14)


## 2.0.1
###### 2016-04-18
### Added
* Identification of Holodeck B2B in _User-Agent_ and _Server_ HTTP headers

### Fixed
* Database doesn't save long meta data strings [#8](https://github.com/holodeck-b2b/Holodeck-B2B/issues/8)
* NPE when using PullRequestFlow without security configuration [#9](https://github.com/holodeck-b2b/Holodeck-B2B/issues/9)
* IOException possible on parallel submission to Holodeck B2B Core [#10](https://github.com/holodeck-b2b/Holodeck-B2B/issues/10)


# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased
### Added
* Option to apply _strict_ validation of ebMS header meta-data. By default Holodeck B2B only validates
  that it is able to process the message. When applying _strict_ validation mode it will check that the header
  meta-data conforms to all requirements as stated in the ebMS Specifications.
  The use of the _strict_ validation mode can be configured both globally on Holodeck B2B instance level
  (`IConfiguation.useStrictHeaderValidation()`) or on a per P-Mode basis (`IPMode.useStrictHeaderValidation()`)
* It is now possible to perform custom validations of _User Message_ message units before delivery to
  the _Consumer_. Depending on the configuration validation problems may result in rejection of the message
  unit and return an ebMS _Error Signal_ to the sender of the message. The configuration of the custom validation
  is done in the _User Message flow_ of the P-Mode, see
  `org.holodeckb2b.interfaces.pmode.IUserMessageFlow.getCustomValidationConfiguration()`
* Interfaces for separation of Core functionality and processing of the WS-Security headers in a message by
  introducing the _Holodeck B2B Security Provider_ concept.
* A default _security provider_ implementation. Similar to the older versions based on the WSS4J libraries
  but now using seperate keystores for encryption and signature verification certificates.
* Message processing events for delivery attempts of message units and security processing, e.g. signature creation and
  validation, en-/decryption, etc.
* Option to indicate that a problem in delivery of the message is permanent and an ebMS Error can be returned to the
  sender of the message.
* Added 'IMessageProcessingState.getDescription()' method so an additional description on the processing state of a
  message unit can be stored. NOTE: This description is introduced for future use, the Holodeck B2B Core does not
  yet use it!

### Changed
* When multiple P-Modes match to a received message this is now considered as "no match found" and no result is
  returned instead of the first matching P-Mode.
* Refactored the validation of the ebMS header meta-data validation classes and handler.
* Renamed method `org.holodeckb2b.interfaces.pmode.validation.IPModeValidator.isPModeValid()` to
  `org.holodeckb2b.interfaces.pmode.validation.IPModeValidator.validatePMode()`
* The ebMS Error returned to the sender in case the signature of a _User Message_ does not include all
  payloads of the message has been changed from _ValueInconsistent_ to _PolicyNonCompliance_
* Moved classes in `org.holodeckb2b.interfaces.pmode.security` to `org.holodeckb2b.interfaces.pmode`
* Refactored package structure of _message processing events_ related interfaces: The definition of all events is done
  using interfaces in the `org.holodeckb2b.interfaces.events` and subpackages. Interfaces related to the processing
  of events are in `org.holodeckb2b.interfaces.eventprocessing`
* Refactored `IQueryManager.isAlreadyDelivered()` to `IQueryManager.isAlreadyProcessed()` to also take failed message
  units into account when determining if a user message is already processed and should be considered a duplicate.

### Deprecated
* The configuration parameter (`IConfiguation.allowSignalBundling()`) which was used to indicate that it was allowed to
  bundle multiple Signal message units of the same type in one message. This goes beyond the ebMS V3 Core and AS4
  Specifications and this option is therefore removed.
* The configuration parameter (`IConfiguation.useStrictErrorRefCheck()`) to apply a strict validation on the
  references in the Error signal is replaced by the generic strict header validation mode.
* The configuration parameters (as defined in `IConfiguration`) related to the keystores used in WS-Security processing.
  By enabling _compatability mode_ in the new default security provider the functionality of the old version is still
  supported.

### Removed
* Deprecated method `IMessageSubmitter.submitMessage(IUserMessage)` and corresponding implementation.

## 3.1.1
###### 2018-01-15
### Fixed
* Notification includes original Receipt content [#82](https://github.com/holodeck-b2b/Holodeck-B2B/issues/82)
* Incorrect root element in single XML file delivery [#83](https://github.com/holodeck-b2b/Holodeck-B2B/issues/83)
* No XML schema provided for single XML delivery [#84](https://github.com/holodeck-b2b/Holodeck-B2B/issues/84)

## 3.1.0
###### 2018-01-10
### Added
* Support for the _type_ attribute of both _Message_ and _Part Properties_. See
  also [issue #2](https://issues.oasis-open.org/browse/EBXMLMSG-2) in the OASIS ebMS TC's issue tracker
* Added the default temp directory to the distribution
* Support for continuously running workers

### Changed
* Packaged the Holodeck B2B msh service as Axis2 aar file
* Included the Holodeck B2B module meta-data in the Core jar file
* Sending of Pull Requests is now done by the `SenderWorker` instead of `PullWorker`

### Fixed
* Reverted back to version 2.0.4 of Apache Santuario library to fix unknown method issue in case
  an exception occurs in the XML security processing
* StringIndexOutOfBoundsException when eb:Timestamp does not contain 'Z' indicator [#78](https://github.com/holodeck-b2b/Holodeck-B2B/issues/78)
* Issue in Receipt processing when the referenced message was not completely loaded from storage
  but previous processing states need to be evaluated
* Pull Requests directly submitted to the Core are never sent
* Non closed output streams and incorrect flushing of data in file delivery methods

## 3.0.1
###### 2017-07-07
### Changed
* Refactored database configuration in default persistency module
* Removed database related code from other modules

### Fixed
* Confusing log message in SubmitFromFile worker when MMD file can not be renamed [#69](https://github.com/holodeck-b2b/Holodeck-B2B/issues/69)
* Body payload(s) not encrypted when message also contains other payloads [#70](https://github.com/holodeck-b2b/Holodeck-B2B/issues/70)

## 3.0.0
###### 2017-06-13
### Added
* P-Mode validation feature which separates the validation of P-Modes from both their internal and external storage
* The direction and processing states of a message unit are now available through the interfaces
* Unit tests to improve test coverage
* Integration tests. These tests set up two Holodeck B2B instances and execute both a push and a pull exchange to check
  that everything works on run time
* A default implementation of the message model interface (in Common module)
* Interfaces for separation of Core functionality and storage of message unit meta-data
* Default _persistency provider_ implementation

### Changed
* Refactored the validation of received message units into separate validation classes and handler
* Refactored Core module to use new persistency interfaces
* Moved XML message meta-data implementation and ProductId classes to Common module
* It is now possible to not only configure whether _Error Signals_ should be logged but also if all signals
  should be logged or only the ones that contain an Error with severity _failure_ (see `log4j2.xml` for more info)

### Removed
* Exceptions `org.holodeckb2b.common.exceptions.DatabaseException` and `org.holodeckb2b.common.exceptions.DuplicateMessageIdError`
  which were bound to the specific persistency implementation of previous versions

### Fixed
* ebMS errors are only logged when log level is ERROR [#35](https://github.com/holodeck-b2b/Holodeck-B2B/issues/35)
* Exceptions in processing of WSS header not handled correctly [#36](https://github.com/holodeck-b2b/Holodeck-B2B/issues/36)
* NPE in CreateSOAPEnvelopeHandler when sending async Error with unknown P-Mode [#45](https://github.com/holodeck-b2b/Holodeck-B2B/issues/45)
* NPE in purge operation when payload location is not available [#46](https://github.com/holodeck-b2b/Holodeck-B2B/issues/46)
* P-Mode finding does not take into account MPC defined in PullRequesFlow [#47](https://github.com/holodeck-b2b/Holodeck-B2B/issues/47)
* NPE in P-Mode finding process when only declaring SecurityConfiguration for a TradingPartner [#48](https://github.com/holodeck-b2b/Holodeck-B2B/issues/48)
* Server starts with error message if Holodeck B2B directory name includes space [#54](https://github.com/holodeck-b2b/Holodeck-B2B/issues/54)
* Unable to start on Windows when using very long base path [#64](https://github.com/holodeck-b2b/Holodeck-B2B/issues/64)
* Resolved build issues in unit tests on Windows platform

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

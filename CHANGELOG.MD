# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 4.1.0-SNAPSHOT
##### Unreleased
### Added
* Support for sending selective pull requests using simple selection items as described in section 5.1 of the
  ebMS 3 Part 2 (Advanced Features) specification. NOTE: 4.1.0 only supports sending of selective pull requests. It does
  not use selection criteria when processing received pull requests. 
* Added two generic events to indicate that a problem occurred during the processing of a received message  
  (`IReceivedMessageProcessingFailure`) or a message to be send (`ISendMessageProcessingFailure`). These event are
  intended for use as _"filters"_ when configuring the event handling. Specific events, are available for specific
  errors. Using the generic events the error reporting defined by the P-Mode parameters  
  **PMode.ErrorHandling.Report.ProcessErrorNotifyProducer** and **PMode.ErrorHandling.Report.ProcessErrorNotifyConsumer**
  can be implemented.
* Added a new event (`org.holodeckb2b.interfaces.events.IHeaderValidationFailure`) to indicate that the validation 
  of the message header failed 
* New interfaces for all _message processing events_ without the _Event_ suffix. The old interfaces are still available
  for backward compatibility, but should not be used anymore (see also below).

### Changed
* When a pulled User Message cannot be matched to a P-Mode it is assigned to the P-Mode of the Pull Request.
* When no MPC is specified in either P-Mode or submission the _default MPC_ is used for the Pull Request. If a MPC is
  specified in both the one in the submission **must** be a sub-channel of the one in the P-Mode.
* The `mpc` attribute is not included in a Pull Request when the _default MPC_ is pulled.
* Generalised the header validation handler so it can be more easily reused for other protocols.
* Removed check on empty _ConversationId_ when submitting a User Message to the Core.
* All event implementation classes to use the new event interface names.
* Split handling of errors generated during processing of a received message in two handlers, one bundling the 
  individual errors into Error Signals and one for determining how to report the Error Signals. Allows for re-use of
  "bundling" handler in different messaging protocols.

### Deprecated
* The interfaces for the  _message processing events_ with the _Event_ suffix. They are replaced with interfaces without
  suffix to shorten the event names and prevent duplication of "event" in the qualifies class name.

### Fixed
* Corrected bug in 4.0.0 that description of processing states could not be saved due to missing API. 
* Name of the log used for reporting errors generated during processing of incoming messages. Did include _null_ instead
  of the message protocol name
* When no retry configuration is available for a message but a Receipt is expected a MissingReceipt ebMS Error is 
  generated and reported [as configured in the P-Mode] to the business application instead of only logging an error 
  message.
* Issue that integration tests were not executed when running them from the IDE and the `JAVA_HOME` system environment 
  variable was not set. Now the JVM used by the IDE is used.
* Strict validation of the AgreementRef being an URI in case no type has been specified.
* Issues in processing User Messages without payloads.
* Check that P-Mode specified for a submitted Pull Request does support pulling and that there is no conflict in the
  MPCs specified in both P-Mode and submission.
* _UnsupportedException_s in `org.holodeckb2b.security.results.SignedPartMetadata.TransformMetadata` when getting 
  algorithm and parameters. 
* Fix issue in not finding the private key for description because alias is given in upper case in P-Mode.

## 4.0.0
##### 2018-11-04
### Added
* More flexible retry configuration of the AS4 Reception Feature where it is now possible to specify each interval
  separately. See refactored `org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness`.
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
* Check on the correct combination of _Service_ and _Action_ meta-data values when when triggering a _"ping test"_ as
  defined in the ebMS V3 Core Specification.
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
* Added optional parameter to `IQueryManager.getMessageUnitsWithId()` to indicate that only message units flowing in
  a specific direction should be returned
* Added default initialization method to `IPModeSet` to pass the Holodeck B2B _home directory_ as parameter
  on creation of the P-Mode storage implementation.

### Changed
* The default URL path where Holodeck B2B receives AS4 messages and which needs to be used by the _Sending MSH_ has been
  changed to _/holodeckb2b/as4_. For backward compatibility the old path _msh_ can still be used, its use however isn't
  recommended!
* When multiple P-Modes match to a received message this is now considered as "no match found" and no result is
  returned instead of the first matching P-Mode.
* Refactored the validation of the ebMS header meta-data validation classes and handler to align with custom validation
  classes.
* Added the Holodeck B2B _home directory_ as parameter to the
  `org.holodeckb2b.interfaces.persistency.IPersistencyProvider.init()` method
* Renamed method `org.holodeckb2b.interfaces.pmode.validation.IPModeValidator.isPModeValid()` to
  `org.holodeckb2b.interfaces.pmode.validation.IPModeValidator.validatePMode()`
* Extracted `Direction` enumeration from `IMessageUnit` into stand alone
* The ebMS Error returned to the sender in case the signature of a _User Message_ does not include all
  payloads of the message has been changed from _ValueInconsistent_ to _PolicyNonCompliance_
* Moved classes in `org.holodeckb2b.interfaces.pmode.security` to `org.holodeckb2b.interfaces.pmode`
* Refactored package structure of _message processing events_ related interfaces: The definition of all events is done
  using interfaces in the `org.holodeckb2b.interfaces.events` and subpackages. Interfaces related to the processing
  of events are in `org.holodeckb2b.interfaces.eventprocessing`
* Refactored `IQueryManager.isAlreadyDelivered()` to `IQueryManager.isAlreadyProcessed()` to also take failed message
  units into account when determining if a user message is already processed and should be considered a duplicate.
* Changed the API specification of the query methods in `IQueryManager` to return **empty** collections instead of
  `null` when no matching message units have been found
* Changed argument of the `IQueryManager.isAlreadyProcessed()` to full _message unit_ instead of just the
  messageId
* Updated to Axis2 version 1.7.7 and Bouncy Castle version 1.59
* The log name for message processing now includes indication of the message protocol being processed. The message
  protocol is retrieved from the _HandledMessagingProtocol_ parameter of the engaged Holodeck B2B module.
* Renamed the Axis2 phases that include the handlers for processing the messages to more generic _protocolInPhase_
  and _protocolOutPhase_ making easier to install extensions for other messaging protocols.
* Refactored the logging to have clear separation of details logged at the different levels. Using _INFO_
  level provides enough information to see all messages which are processed. Going to _DEBUG_ add information
  of process steps and _TRACE_ provides most detailed logging.    
* In case of exception when logging an invalid SOAP message, a error message is written to SOAP log

### Deprecated
* The configuration parameter (`IConfiguation.allowSignalBundling()`) which was used to indicate that it was allowed to
  bundle multiple Signal message units of the same type in one message. This goes beyond the ebMS V3 Core and AS4
  Specifications and this option is therefore removed.
* The configuration parameter (`IConfiguation.useStrictErrorRefCheck()`) to apply a strict validation on the
  references in the Error signal is replaced by the generic strict header validation mode.
* The configuration parameters (as defined in `IConfiguration`) related to the keystores used in WS-Security processing.
  By enabling _compatability mode_ in the new default security provider the functionality of the old version is still
  supported.
* The methods `getMaxRetries()` and `getRetryInterval()` in `org.holodeckb2b.interfaces.as4.pmode.IReceptionAwareness`
  as they are superseded by the new `getWaitIntervals()` method which allows both fixed and flexible retry
  configuration. Older implementations of this interface can still be used in this version as it provides _default_
  implementations to convert to the new interface.
* The _msh_ URL path for receiving messages. The new _/holodeckb2b/as4_ path should be used.

### Removed
* Method `IMessageSubmitter.submitMessage(IUserMessage)` and corresponding implementation.
* Method `IPModeSet.listPModeIds()` and corresponding implementation.

## Unreleased
### Changed
* Use 'exec' to start the Java program, instead of forking the process to simplify management of
  the Java process.
* Updated the example certificates and key stores to extend validity of certificates

### Fixed
* Checking of references in Error Signal has been corrected so Error with the reference contained
  only in the Errors is not rejected.
* Exception when processing an invalid SOAP message [#15](https://github.com/holodeck-b2b/Holodeck-B2B/issues/15)
* Updated README regarding certificate management in `repository/certs` folder to include also
  the keystore with the certificates of trusted certificate authorities.
* Severity attributes should be "warning" or "failure" not "WARNING" or "FAILURE" [#89](https://github.com/holodeck-b2b/Holodeck-B2B/issues/89)

## 3.1.2
##### 2018-05-28
### Changed
* Test for `org.holodeckb2b.ebms3.workers.SubmitFromFile` worker has been changed due to refactoring

### Fixed
* Skipped MMD files ignored until gateway restart [#86](https://github.com/holodeck-b2b/Holodeck-B2B/issues/86)

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
* Extended `IMessageSubmitter.submitMessage(IUserMessage)` method with parameter to indicate whether to delete the
  original payload files on message submission  

### Changed
* Refactored the validation of received message units into separate validation classes and handler
* Refactored Core module to use new persistency interfaces
* Moved XML message meta-data implementation and ProductId classes to Common module
* It is now possible to not only configure whether _Error Signals_ should be logged but also if all signals
  should be logged or only the ones that contain an Error with severity _failure_ (see `log4j2.xml` for more info)

### Deprecated
* The single parameter version of the `IMessageSubmitter.submitMessage(IUserMessage)` method has been deprecated. The
  new version with the additional parameter to indicate whether payload files should be deleted should be used.

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

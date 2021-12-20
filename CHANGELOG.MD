# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 5.3.2
##### 2021-12-20
### Fixed
** Vulnerability in log4j2, see [CVE-2021-45105](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45105)

## 5.3.1
##### 2021-12-13
### Changed
* Removed ".." from directory names on Windows

### Fixed
* Vulnerability in log4j2 [#123](https://github.com/holodeck-b2b/Holodeck-B2B/issues/123)


## 5.3.0
##### 2021-10-25
### Added
* New processing state _SUSPENDED_ to indicate that an error occurred that prevented the sending of a User Message to 
  the trading partner and the possibility to resume their processing by calling the 
  `HolodeckB2BCoreInterface#resumeProcessing()` method.
* Option to have multiple event handlers for one event. See `IMessageProcessingEventConfiguration`
* `ISendMessageProcessingFailure` is now also raised when a message unit cannot be (re)sent, i.e. before the actual
  send process is started.
  
### Changed
* Improved shutdown process in `org.holodeckb2b.core.HolodeckB2BServer`. Includes support for [Apache commons daemon|
https://commons.apache.org/proper/commons-daemon/index.html] (using _jvm_ mode).
* Message properties are included in P-Mode matching, i.e. all properties defined in a P-Mode must be specified in the
  received message for a P-Mode to match. But any properties in the message not defined in the P-Mode are ignored.

### Fixed
* Delivery events (`IMessageDelivered` and `IMessageDeliveryFailure`) not raised for delivered Signal Messages.
* Getting send and receive legs for 2-Way P-Mode does not work for non ebMS3 MEP bindings.

## 5.2.0
##### 2021-09-02
### Added
* `org.holodeckb2b.core.axis2.HTTPWorker` puts the request parameters included in the HTTP URL in _MessageContext_
  property "hb2b:" + `org.apache.axis2.Constants.REQUEST_PARAMETER_MAP`. The property value is a `Map<String, String>` 
  of parameter names to values.

### Changed
* Removed Core Axis2 module as default. 
* Maven ArtifactId of the default ebMS3 Security Provider module to make it consistent with other module names.
* Log message when no event handlers are configured in P-Mode.

### Fixed
* Duplicate messages not eliminated [#117](https://github.com/holodeck-b2b/Holodeck-B2B/issues/117) 
* Setting of threads available for a _worker pool_  
* NPE when writing P-Mode XML document with retry configuration

## 5.1.0
##### 2021-05-26
### Added
* Management of worker pools by the Core, including the possibility for "on the fly" reconfiguration. For this purpose a
 new `IWorkerPool` interface has been introduced and methods in the `HolodeckB2BCoreInterface` and 
 `IWorkerPullConfiguration` have been added.

### Changed
* Pull reconfiguration is now managed by the worker pool and configured in the pull configuration file by setting the
  _refresh_ parameter
* Loading of the _Event Processor_, _Certificate Manager_, _Persistency Provider_ and ebMS3 _Security Provider_ has been
  changed to load the first available provider instead of trying to load just the first one registered
* Improved logging of unexpected errors during message processing 
* Refactored code to use the generic utility classes from https://github.com/holodeck-b2b/generic-utils
* Moved `AbstractWorkerTask` class from `org.holodeckb2b.common.workerpool` to `org.holodeckb2b.common.workers`
* Renamed `org.holodeckb2b.common.workerpool`to `org.holodeckb2b.core.workerpool`

### Deprecated
* `org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration.getName()` as worker pool names are now set in code

### Fixed
* Finding the correct P-Mode for User Messages received on response leg of a Two-Way P-Mode
* Completion of submission  for Two-Way P-Modes 
* Processing of a received message with more than 30 attachments fails [#113](https://github.com/holodeck-b2b/Holodeck-B2B/issues/113) 
* Setting of HTTP response code in response message context

### Removed
* The _pullConfigWatcher_ as the pull configuration is now automatically refreshed by the worker pool itself 
* Generic utility classes `org.holodeckb2b.common.util.Utils`, `org.holodeckb2b.common.util.MessageIdUtils` and
 `org.holodeckb2b.security.util.KeystoreUtils` as they are moved to a new, separate project.


## 5.0.3
##### 2020-11-18
### Fixed
* Multihop routing information missing in async Signal Messages [#112](https://github.com/holodeck-b2b/Holodeck-B2B/issues/112) 

## 5.0.2
##### 2020-09-16

### Changed
* Included version 1.0.1 of the file back-end which fixes in the _single\_xml_ delivery format and uses a temporary
  extension when writing the meta-data file to disk to prevent premature reading by the back-end application. See
  also the [issues on the file back-end project](https://github.com/holodeck-b2b/file-backend/issues?q=is%3Aissue).

## 5.0.1
##### 2020-09-03

### Changed
* Renewed example certificates
* Updated example P-Modes to use new file delivery method name

### Fixed
* Backward compatibility for "relaxed" reading P-Mode XML documents.
* P-Modes and certificates are now refreshed when switching tabs in the GUI [#108](https://github.com/holodeck-b2b/Holodeck-B2B/issues/108) 
* Non AS4 P-Modes evaluated for AS4 messages [#111](https://github.com/holodeck-b2b/Holodeck-B2B/issues/111) 


## 5.0.0
##### 2020-07-03

### Added
* A default user interface that supports basic monitoring of a running Holodeck B2B instance. The UI is available both
  using a command line and GUI application.
* Support for asynchronous Two-Way AS4 P-Modes.
* Support for additional P-Mode validators for the same type of P-Mode. _P-Mode validators_ are now loaded using the Java 
  SPI mechanism and all applicable validators are used to check a P-Mode before loading it. Two new methods were added
  in `org.holodeckb2b.interfaces.pmode.validation.IPModeValidator` to support the new mechanism.  
* Configuration parameter to indicate whether the Holodeck B2B Core should fall back to default _Event Processor_ 
  implementation in case the custom implementation cannot be loaded/initialised. 
* New interfaces related to trust validation of certificates in `org.holodeckb2b.interfaces.security.trust`. Notably
  the new specification of the Holodeck B2B _Certificate Manager_ which is now a separate component independent of the
  _Security Provider_. Also new interfaces and methods are defined to communicate the results of trust validation checks
  between components including the new `ISignatureVerifiedWithWarning` event to signal trust issues on signature
  verification.
* Signature policy check in **Core Processing** that received _User Messages_ are signed when indicated in P-Mode that 
  they should be. Violation of this rule will generate an _PolicyNonCompliance_ error.
* On submission of _User Message_  a check that the provided payload _Content-Id_ values do not contain invalid characters.
* Added new message processing events to indicate delivery or failure of a message unit to the back-end application.
* When an (unexpected) error occurs during the processing of a message unit a `ISendMessageProcessingFailure` or 
  `IReceiveMessageFailure` is raised.  
* `org.holodeckb2b.interfaces.core.IMessageProcessingContext` interface which can be used in extension APIs to provide 
  access to the HB2B _message processing context_.  
* Custom implementation of the Axis2 `TransportListener` interface which can use a _Service_ specified _Message Builder_.  
* Option to add HTTP response headers to an empty response by setting a property named `HTTPConstants.RESPONSE_HEADERS` 
  on the request MessageContext containing Map of name and value of headers to set.    
* `org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface.getVersion()` method and 
  `org.holodeckb2b.interfaces.general.IVersionInfo` interface to provide information about the version of the 
  Holodeck B2B instance.
* `org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface.getModule(String)` method to access an active Axis2 Module
  on the Holodeck B2B instance. This can be used by extensions to get access to "their" module. 
* `org.holodeckb2b.common.messagemodel.MessageUnit.copyOf(IMessageUnit)` to create a copy of the message unit data in
  an object of the common message model implementation. 
* Default implementation of the new `ICertificateManager` and `IValidationResult` trust interface to the _security_ 
  module.
* Option to configure the directory where the Holodeck B2B message database should be stored through the `HB2B_DB_DIR`
  environment variable.

### Changed
* P-Modes are now by default stored in the `repository/pmodes` directory.  
* The Axis2 configuration has been merged into the `holodeckb2b.xml` configuration file. 
* The default key transport algorithms have been upgraded to RSA-OAEP and MGF1 with SHA256.
* Reception Awareness is now a generic feature not bound to AS4. As a result the `getReceptionAwareness()` method has
  moved to `org.holodeckb2b.interfaces.pmode.ILeg` and class `IReceptionAwareness` has moved to the 
  `org.holodeckb2b.interfaces.pmode` package.
* Split the `IMessageDelivery` and `IMessageTransfer` events into two separate events for success and failure.    
* Startup sequence now checks that the server is correctly started and aborts startup if not.
* The _Security Provider_,_Persistency Provider_, _P-Mode storage_ and _Event Processor_ are now loaded using the Java 
  _Service Provider Interface_ mechanism.   
* Refactored the interfaces of the _Persistency Provider_. The ` IDAOFactory` interface has been removed and its methods
  are now defined directly in the `PersistencyProvider` interface.
* The security provider interface now use the new `org.holodeckb2b.interfaces.core.IMessageProcessingContext` interface
  to provide access to the processing context.
* Restructured the project's modules to create a better separation of _Core_ and _protocol specific_ code. This includes 
  renaming of Axis2 phases to reflect the difference between _Core_ and _protocol specific_ processing.
* Split the _security_ module into two sub modules; one to implement the new default _Certificate Manager_ and one to 
  implement the _Security Provider_. Both implementations are refactored version of the old _Security Provider_ classes.
* Refactored the default P-Mode implementation so it also supports setting of parameters and serialization to XML.
* Refactored send process to make it more flexible and use *PMode.MEPBinding* to select the Axis2 _Service_ to use for
  sending. This removes the need for coded _Services_ and specialised senders or sender workers when implementing an
  additional messaging protocol.
* Refactored the `OutOptInAxisOperation` so it will also use the _Service_ specified _Message Builder_ for building the
  response message.  
* Renamed abstract worker implementations to include _Abstract_ prefix.
* Moved classes in `org.holodeckb2b.common.messagemodel.util` to generic utility package `org.holodeckb2b.common.util`.  
* Moved _PModeWatcher_ worker to the `org.holodeckb2b.common.pmode` package as it's directly related to the XML P-Mode
  implementation contained in this package.
* Constructor of `org.holodeckb2b.interfaces.submit.MessageSubmitException` now accepts `Throwable` instead of 
  `Exception`. 
* The classes of the common message model implementation are now `Serializable`.  
* Switched to Log4J2 logging in handlers.

### Fixed
* Generated Content-Id results in signature failure [#99](https://github.com/holodeck-b2b/Holodeck-B2B/issues/99).
* NPE when white space is contained between the `ds:Signature` and `ds:SignedInfo` elements of a received message 
 [#100](https://github.com/holodeck-b2b/Holodeck-B2B/issues/100)
* Error descriptions longer than 255 characters are not saved.
* NPE occurs on event handlers with no parameters [#102](https://github.com/holodeck-b2b/Holodeck-B2B/issues/102)
* Invalid SOAPFault included with ebMS Error using SOAP 1.2 [#103](https://github.com/holodeck-b2b/Holodeck-B2B/issues/103)
* Inconsistency in reception awareness [#104](https://github.com/holodeck-b2b/Holodeck-B2B/issues/104)
* NPE for submitted UserMessage without CollaborationInfo [#105](https://github.com/holodeck-b2b/Holodeck-B2B/issues/105)

### Removed
* Message processing events which names ended on _Event_.
* Setter methods in `org.holodeckb2b.interfaces.general.IAgreement`.
* The configuration parameter (`IConfiguation.useStrictErrorRefCheck()`) to apply a strict validation on the
  references in the Error signal.
* The following configuration parameters (as defined in `IConfiguration`) have been removed:
    * Related to the keystores used in WS-Security processing and certificate management.
    * P-Mode validator class (now loaded through SPI mechanism)
    * Security Provider class (now loaded through SPI mechanism)
    * Persistency Provider class (now loaded through SPI mechanism)
    * P-Mode set storage class (now loaded through SPI mechanism)
* The _msh_ URL path for receiving messages.
* Split the file based back-end integration into a separate project (see [File-backend](https://github.com/holodeck-b2b/file-backend)).
  However this back-end is still included in the default distribution. 
* `org.holodeckb2b.interfaces.config.IConfiguration.getAxisConfigurationContext()` method, as extensions should not need 
  access to internal Axis configuration.
* `org.holodeckb2b.interfaces.entities.IMessageUnit.getLeg()`, as the leg can be calculated based on the P-Mode.
* Deprecated methods from `IReceptionAwarness`

## 4.1.2
##### 2019-04-10
### Fixed
* Incorrectly generated IDs within WS-Security header [#98](https://github.com/holodeck-b2b/Holodeck-B2B/issues/98)

## 4.1.1
##### 2019-04-04
### Fixed
* Misleading result of PullRequest submission [#91](https://github.com/holodeck-b2b/Holodeck-B2B/issues/91)
* NPE on first initialization of pull workers [#92](https://github.com/holodeck-b2b/Holodeck-B2B/issues/92)
* Disk resource leakage when submission fails [#94](https://github.com/holodeck-b2b/Holodeck-B2B/issues/94)
* Trusted certificate with name constraints extension results in failed processing [#97](https://github.com/holodeck-b2b/Holodeck-B2B/issues/97)

### Changed
* The default security provider now uses version 2.2.2 and 2.1.2 of WSS4J and Apache Santuario (xml-sec)

### Removed
* `org.holodeckb2b.interfaces.submit.IMessageSubmitterFactory` interface

## 4.1.0
##### 2019-03-06
### Added
* Support for sending _selective pull requests_ using simple selection items as described in section 5.1 of the
  ebMS 3 Part 2 (Advanced Features) specification.   
  NOTE: In version 4.1.0 the support is limited to sending of selective pull requests with "simple selection items"
  as described in the specification. Selection criteria are not used when processing received pull requests. 
* Added two generic events to indicate that a problem occurred during the processing of a received message  
  (`IReceivedMessageProcessingFailure`) or a message to be sent (`ISendMessageProcessingFailure`). These events are
  intended for use as _"filters"_ when configuring the event handling. Specific events are available for specific
  errors. Using the generic events the error reporting defined by the P-Mode parameters  
  **PMode.ErrorHandling.Report.ProcessErrorNotifyProducer** and **PMode.ErrorHandling.Report.ProcessErrorNotifyConsumer**
  can be implemented.
* Added a new event (`org.holodeckb2b.interfaces.events.IMessageSubmission`) to indicate that a _User Message_ or _Pull
  Request_ message unit was submitted to the Holodeck B2B Core 
* Added a new event (`org.holodeckb2b.interfaces.events.IHeaderValidationFailure`) to indicate that the validation 
  of the message header failed 
* New interfaces for all _message processing events_ without the _Event_ suffix. The old interfaces are still available
  for backward compatibility, but should not be used anymore (see also below).
* Option to register "_global event handlers_" in the Holodeck B2B Core. These handlers will be used to process events 
  if the P-Mode does not specify one.
* A Holodeck B2B specific _message processing context_ that holds all information about the message that is processed
  in the current processing pipeline. Refactored all handler classes to use new context.  

### Changed
* When a pulled User Message cannot be matched to a P-Mode it is assigned to the P-Mode of the Pull Request.
* When no MPC is specified in either P-Mode or submission the _default MPC_ is used for the Pull Request. If an MPC is
  specified in both the one in the submission **must** be a sub-channel of the one in the P-Mode.
* The `mpc` attribute is not included in a Pull Request when the _default MPC_ is pulled.
* Generalised the header validation handler so it can be more easily reused for other protocols.
* Removed check on empty _ConversationId_ when submitting a User Message to the Core.
* All event implementation classes to use the new event interface names.
* Split handling of errors generated during processing of a received message in two handlers, one bundling the 
  individual errors into Error Signals and one for determining how to report the Error Signals. Allows for re-use of
  "bundling" handler in different messaging protocols.
* Moved common classes from core to the common module.
* Core submission function now first checks whether the specified payloads can be copied/moved to internal storage 
  before saving the message meta-data. This allows back-end applications to re-submit messages in case there is an error
  in moving the payloads.

### Deprecated
* The interfaces for the  _message processing events_ with the _Event_ suffix. They are replaced with interfaces without
  suffix to shorten the event names and prevent duplication of "event" in the qualified class name.

### Fixed
* Description of processing states could not be saved due to missing API. (In Holodeck B2B 4.0.0.)  
* Name of the log used for reporting errors generated during processing of incoming messages included _null_ instead
  of the message protocol name.
* When no retry configuration is available for a message but a Receipt is expected a MissingReceipt ebMS Error is 
  generated and reported [as configured in the P-Mode] to the business application instead of only logging an error 
  message.
* Integration tests were not executed when running them from the IDE with the `JAVA_HOME` system environment 
  variable not set. Now the JVM used by the IDE is used.
* Strict validation of the AgreementRef being a URI in case no type has been specified.
* Exceptions in processing User Messages without payloads.
* Check that P-Mode specified for a submitted Pull Request supports pulling and that there is no conflict in the
  MPCs specified in both P-Mode and submission.
* _UnsupportedException_s in `org.holodeckb2b.security.results.SignedPartMetadata.TransformMetadata` when getting 
  algorithm and parameters. 
* Issue in finding P-Mode for User Messages when message received could be matched to a P-Mode configured for sending
  messages based on pull.   
* NPE in file delivery of _Receipt_ without content (will only occur when used for non-AS4 Receipts)
* NPE when ebMS3 message is received that contains a WS-Security Signature which does not sign the ebMS message header
  (i.e. there is no `ds:Reference` element for the `eb3:Messaging` element)       

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

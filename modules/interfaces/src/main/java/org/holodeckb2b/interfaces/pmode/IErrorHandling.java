/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.pmode;


import org.holodeckb2b.interfaces.delivery.IDeliverySpecification;
import org.holodeckb2b.interfaces.general.ReplyPattern;

/**
 * Describes the P-Mode parameters for handling errors.
 * <p>Error handling is described in section 6.6 of the ebMS V3 Core Specification and includes three options for 
 * reporting errors: using SOAP Faults, out-of-band notifications and using ebMS Errors. Holodeck B2B will always log 
 * detected errors, including received error signals, enabling out-of-band notifications.<br>
 * Furthermore errors can be reported using ebMS errors. This interface specifies the configuration parameters for the 
 * reporting and is based on the P-Mode parameter group <b>PMode[1].ErrorHandling.Report</b> defined in appendix D of 
 * the Core Specification.
 * 
 * <p>This version focuses on reporting errors to the sender of the message in error. As a result there is no support 
 * for the P-Mode parameters <b>SenderErrorsTo</b> and <b>ProcessErrorNotifyConsumer</b>. Also 
 * <b>DeliveryFailuresNotifyProducer</b> is not supported as there is no support for WS-Reliability or WS-RM in this 
 * version of Holodeck B2B.
 * 
 * <p>The parameter <b>ProcessErrorNotifyProducer</b> defined in the Core Specification is represented by the 
 * {@link #shouldNotifyErrorToBusinessApplication()} that indicates whether the business application should be informed 
 * on errors for a sent message. If this method returns <i>true</i> either {@link #getErrorDelivery()} or 
 * {@link ILeg#getDefaultDelivery()} must return a {@link IDeliverySpecification} that specifies how the errors should 
 * be delivered to the business application.
 *
 * <p>According to the ebMS V3 Core Specification an ebMS error of severity <i>failure</i> must always be combined with
 * a SOAP Fault. As noted in <a href="https://issues.oasis-open.org/browse/EBXMLMSG-4">issue #4 in the OASIS ebMS TC's 
 * issue tracker</a> adding the SOAP Fault should be optional. By default Holodeck B2B will not add a SOAP Fault to 
 * to ebMS error messages that contain an error with severity <i>failure</i>, but with {@link #shouldAddSOAPFault()} 
 * this can be overridden in the P-Mode.
 * 
 * <p>Not specified in the ebMS V3 or AS4 specifications is whether errors generated for received error or receipts 
 * should be reported back to the sender of the message in error. In most cases the problem with such errors will be 
 * that they can not be correctly related to an existing message resulting in unknown P-Mode for that error. Therefor 
 * error reporting on such errors is configured globally (see {@link Config#shouldReportErrorOnError()} and 
 * {@link Config#shouldReportErrorOnReceipt()}). But it is possible to overwrite these global settings using the P-Mode.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IErrorHandling {

    /**
     * Gets the indication how Error signals should be sent: as a callback, or synchronously on the HTTP back-channel,
     * i.e. as the response.
     * <p>Note that when the error should be sent via a callback the address where to deliver the error must
     * also be specified, i.e. {@link #getReceiverErrorsTo()} MUST return a URL.
     * 
     * @return  {@link ReplyPattern#CALLBACK} when the error signal should be sent as a callback,<br>
     *          {@link ReplyPattern#RESPONSE} when the error signal should be sent as a response.
     */
    public ReplyPattern getPattern();
    
    /**
     * Gets the URL where error signals should be sent to in case the <i>callback</i> reply pattern is used.
     * 
     * @return The URL where to sent error signals to
     */
    public String getReceiverErrorsTo();
    
    /**
     * Gets the indication whether a SOAP Fault should be added to error messages that contain an error with severity
     * <i>failure</i>.
     * <p>NOTE : Holodeck B2B by default does not add SOAP Fault, so this option should only be used to override this.
     * <p>NOTE : Even when this method returns <i>true</i> the SOAP Fault may not be added if the ebMS message contains
     *           other message units. 
     * 
     * @return <i>true</i> when a SOAP Fault should be added to ebMS error messages that contain an error with
     *          severity <i>failure</i>,<br>
     *         <i>false</i> or <code>null</code> if no SOAP Fault should be added.
     */
    public Boolean shouldAddSOAPFault();
    
    /**
     * Gets the indication whether errors generated for received error messages should be reported back to the sender
     * of the error message in error.   
     * <p>NOTE : This setting only applies to error messages that can be related to this P-Mode! Otherwise the default
     * setting will apply.
     * <p>NOTE : Enabling this option can lead to a loop of error messages if the other MSH will also report errors on 
     * errors and does not understand the reported error. 
     * 
     * @return <i>true</i> if generated errors on errors should be reported to the sender of the error,<br>
     *         <i>false</i> or <code>null</code> otherwise 
     */
    public Boolean shouldReportErrorOnError();
    
    /**
     * Gets the indication whether errors generated for received receipt messages should be reported back to the sender
     * of the receipt message in error.  
     * <p>NOTE : This setting only applies to error messages that can be related to this P-Mode! Otherwise the default
     * setting will apply.
     * 
     * @return <i>true</i> if generated errors on receipts should be reported to the sender of the receipt,<br>
     *         <i>false</i> or <code>null</code> otherwise 
     */
    public Boolean shouldReportErrorOnReceipt();
    
    /**
     * Gets the indication whether the connected business application should be notified in case an error is generated
     * for a sent message.
     * <p>This corresponds to the <b>PMode[1].ErrorHandling.Report.ProcessErrorNotifyProducer</b> parameter defined in
     * the ebMS V3 Core Specification.
     * <p>If the business application should be informed on errors this is done using the standard <i>message delivery 
     * mechanism</i> which is configured by a {@link IDeliverySpecification}. If needed a specific delivery can be 
     * configured for errors by providing it through {@link #getErrorDelivery()}.
     * 
     * @return  <code>true</code> when the business application should be notified on errors,<br>
     *          <code>false</code> when the business application does not need to be notified on errors.
     */
    public boolean shouldNotifyErrorToBusinessApplication();
    
    /**
     * Get the configuration for the delivery specific to error messages.
     * <p>When the default message delivery (configured on the leg, see {@link ILeg#getDefaultDelivery()}) can be used 
     * this method SHOULD return <code>null</code>. 
     * 
     * @return  The {@link IDeliverySpecification} to use for reporting errors to the business application, or<br>
     *          <code>null</code> if the default delivery must be used for reporting errors
     */
    public IDeliverySpecification getErrorDelivery();
}

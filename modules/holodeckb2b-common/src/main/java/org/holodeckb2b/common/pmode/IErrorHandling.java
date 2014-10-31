/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.holodeckb2b.common.pmode;

import org.holodeckb2b.common.delivery.IDeliverySpecification;
import org.holodeckb2b.common.general.ReplyPattern;

/**
 * Describes the P-Mode parameters for reporting errors. Error reporting is described in section 6.6 of the ebMS V3 Core 
 * Specification and includes three options for reporting errors: using SOAP Faults, out-of-band notifications and using
 * ebMS Errors. Holodeck B2B will always log detect errors, including received error signals, enabling out-of-band 
 * notifications. Furthermore errors can be reported using ebMS errors. This interface specifies the configuration 
 * parameters for the reporting.
 * <p>The error reporting parameters are a modified version of the P-Mode parameter group ErrorHandling as defined in 
 * appendix D of the Core Specification. For reporting errors generated for received messages the parameters 
 * <i>pattern</i> and <i>receiverErrorsTo</i> define how the errors should be transmitted to the sender.<br>
 * The other parameters defined in the Core Specification have been replaced by the new boolean parameter 
 * <i>notifyErrorToBusinessApplication</i> that indicates whether the business application should be informed on errors
 * for a sent message. How the errors should be delivered to the business application can be specified in a  
 * {@link IDeliverySpecification}.<br>
 * Not specified in the ebMS V3 or AS4 specifications is whether errors generated for received error or receipts should
 * be reported back to the sender of the message in error. In most cases the problem with such errors will be that they 
 * can not be correctly related to an existing message resulting in unknown P-Mode for that error. Therefor error 
 * reporting on such errors is configured globally (see ). But it is possible to overwrite these global settings using
 * the P-Mode.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IErrorHandling {

    /**
     * Gets the indication how Error signals should be sent: as a callback, or synchronously in the back-channel 
     * response. Note that when the error should be sent via a callback the address where to deliver the error must
     * also be specified.
     * 
     * @return  {@link ReplyPattern#CALLBACK} when the error signal should be sent as a callback,<br>
     *          {@link ReplyPattern#RESPONSE} when the error signal should be sent as a response.
     */
    public ReplyPattern getPattern();
    
    /**
     * Gets the URI where error signals should be sent to in case the <i>callback</i> reply pattern is used.
     * 
     * @return The URI where to sent error signals to
     */
    public String getReceiverErrorsTo();
    
    /**
     * Gets the indication whether errors generated for received error messages should be reported back to the sender
     * of the error message in error.   
     * <p>NOTE : This setting only applies to error messages that can be related to this P-Mode! Otherwise the default
     * setting will apply.
     * <p>NOTE : Enabling this option can lead to a loop of error messages if the other MSH will also report errors on 
     * errors and does not understand the reported error. 
     * 
     * @return <code>true</code> if generated errors on errors should be reported to the sender of the error,<br>
     *         <code>false</code> otherwise 
     */
    public Boolean shouldReportErrorOnError();
    
    /**
     * Gets the indication whether errors generated for received receipt messages should be reported back to the sender
     * of the receipt message in error.  
     * <p>NOTE : This setting only applies to error messages that can be related to this P-Mode! Otherwise the default
     * setting will apply.
     * 
     * @return <code>true</code> if generated errors on receipts should be reported to the sender of the receipt,<br>
     *         <code>false</code> otherwise 
     */
    public Boolean shouldReportErrorOnReceipt();
    
    /**
     * Gets the indication whether the connected business application should be notified in case an error is generated
     * for a sent message. 
     * <p>If the business application should be informed on errors this is done using standard <i>message delivery 
     * mechanism</i> which is configured by a {@link IDeliverySpecification}. If needed a specific delivery can be 
     * configured for errors.
     * 
     * @return  <code>true</code> when the business application should be notified on errors,<br>
     *          <code>false</code> when the business application does not need to be notified on errors.
     */
    public boolean shouldNotifyErrorToBusinessApplication();
    
    /**
     * Get the configuration for the delivery specific to error messages. When the default message delivery (configured
     * on the leg, see {@link ILeg#getDefaultDelivery()}) can be used this method SHOULD return <code>null</code>. 
     * 
     * @return  The {@link IDeliverySpecification} to use for reporting errors to the business application, or<br>
     *          <code>null</code> if the default delivery must be used for reporting errors
     */
    public IDeliverySpecification getErrorDelivery();
}

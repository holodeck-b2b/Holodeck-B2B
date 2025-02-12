/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.axis2;

import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.events.impl.GenericSendMessageFailure;
import org.holodeckb2b.common.util.MessageUnitUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.HolodeckB2BCore;
import org.holodeckb2b.core.MessageProcessingContext;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.core.IMessageProcessingContext;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;
import org.holodeckb2b.interfaces.storage.IMessageUnitEntity;
import org.holodeckb2b.interfaces.storage.IPullRequestEntity;
import org.holodeckb2b.interfaces.storage.IReceiptEntity;
import org.holodeckb2b.interfaces.storage.IUserMessageEntity;
import org.holodeckb2b.interfaces.storage.providers.StorageException;

/**
 * Is responsible for sending the message unit using the Axis2 framework. Depending on the messaging protocol the
 * correct <i>Service</i> is chosen to ensure that the correct handlers are invoked. The <i>Service</i> to use is
 * determined based on the value of the <b>PMode.MEPBinding</b> parameter. If its value starts with <i>
 * "http://holodeck-b2b.org/pmode/mepBinding/"</i> this class will look for a registered service with the name that
 * matches the next path segment of the MEPBinding value. Examples: For "http://holodeck-b2b.org/pmode/mepBinding/as2"
 * the expected Service is "as2" and for "http://holodeck-b2b.org/pmode/mepBinding/ebms2/oneWay" the Service is "ebms2".
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Axis2Sender {
	private final static Logger log = LogManager.getLogger(Axis2Sender.class);

    /**
     * Sends the given message unit to the other MSH.
     *
     * @param messageUnit   The message unit to send
     * @throws StorageException when the processing state could not be updated
     */
    public static void sendMessage(final IMessageUnitEntity messageUnit) throws StorageException {
        log.trace("Starting send operation for {} (msgId={})", MessageUnitUtils.getMessageUnitName(messageUnit),
        			messageUnit.getMessageId());
        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(messageUnit.getPModeId());
        if (pmode == null) {
        	log.error("Cannot send {} [msgId={}] because associated P-Mode {} is not available!",
        				MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(),
        				messageUnit.getPModeId());
        	registerSendFailure(messageUnit, "P-Mode not available", null);
        	return;
        }
        log.trace("Check PMode.MEPBinding which Service should be used");
        // The default protocol is AS4, so we will use the "as4" Service unless P-Mode indicates something else
        String svcName = "as4";
        final String mepBinding = pmode.getMepBinding();
        if (mepBinding.startsWith("http://holodeck-b2b.org/pmode/mepBinding/")) {
        	int segmentEnd = mepBinding.indexOf('/', 41);
        	if (segmentEnd < 0)
        		svcName = mepBinding.substring(41);
        	else
        		svcName = mepBinding.substring(41, segmentEnd);
        }

        final ConfigurationContext configContext = new ConfigurationContext(HolodeckB2BCore.getConfiguration());
        final AxisConfiguration axisConfig = configContext.getAxisConfiguration();
        AxisService service;
        try {
			service = axisConfig.getService(svcName);
		} catch (AxisFault e) {
			service = null;
		}
        if (service == null) {
        	log.error("Cannot send {} [msgId={}] because required {} Service is not installed!",
    				   MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(),
    				   svcName);
        	registerSendFailure(messageUnit, "Required messaging service not installed", null);
        	return;
        }
        log.debug("Using {} Service to send {} [msgId={}]", svcName, MessageUnitUtils.getMessageUnitName(messageUnit),
        			messageUnit.getMessageId());

        final MessageContext msgCtx = new MessageContext();
        msgCtx.setFLOW(MessageContext.OUT_FLOW);
        OperationClient oc = null;

        try {
	        AxisServiceGroup axisServiceGroup = service.getAxisServiceGroup();
	        ServiceGroupContext sgc = configContext.createServiceGroupContext(axisServiceGroup);
	        ServiceContext svcCtx = sgc.getServiceContext(service);

	        final Options options = new Options();
	        options.setTransportOut(axisConfig.getTransportOut("http"));
	        options.setExceptionToBeThrownOnSOAPFault(false);
	        options.setProperty(HTTPConstants.USER_AGENT, Axis2Utils.HTTP_PRODID_HEADER);
	        OutInAxisOperation sendOp = new OutOptInAxisOperation(ANON_OUT_IN_OP);
	        sendOp.setParent(service);
	        axisConfig.getPhasesInfo().setOperationPhases(sendOp);
	        // Engage all modules required by the service
	        for(String moduleName : service.getModules()) {
	            AxisModule module = axisConfig.getModule(moduleName);
	            if (module != null)
	                sendOp.engageModule(module);
	        }
	        oc = sendOp.createClient(svcCtx, options);
	        oc.addMessageContext(msgCtx);

	        log.trace("Create an empty IMessageProcessingContext for message with current configuration");
            final IMessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(msgCtx);
            if (messageUnit instanceof IUserMessage)
                procCtx.setUserMessage((IUserMessageEntity) messageUnit);
            else if (messageUnit instanceof IPullRequest)
            	procCtx.setPullRequest((IPullRequestEntity) messageUnit);
            else if (messageUnit instanceof IErrorMessage)
                procCtx.addSendingError((IErrorMessageEntity) messageUnit);
            else if (messageUnit instanceof IReceipt)
                procCtx.addSendingReceipt((IReceiptEntity) messageUnit);
        } catch (AxisFault cfgError) {
        	log.error("An exception occurred setting up the send operation for {} (msgId={}).Exception stack below:\n",
        			   MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(),
        			   Utils.getExceptionTrace(cfgError, true));
        	registerSendFailure(messageUnit, "Axis2 initialisation failure", cfgError);
        	return;
        }

        try {
        	log.debug("Start send process for {} [msgId={}]", MessageUnitUtils.getMessageUnitName(messageUnit),
        				messageUnit.getMessageId());
            oc.execute(true);
            log.debug("Finished send process for {} [msgId={}]", MessageUnitUtils.getMessageUnitName(messageUnit),
            		messageUnit.getMessageId());
        } catch (final Throwable t) {
            /* An error occurred while sending the message, it should however be already processed by one of the
               handlers. In that case the message context will not contain the failure reason. To prevent redundant
               logging we check if there is a failure reason before we log the error here.
            */
        	if (msgCtx.getFailureReason() != null) {
        		log.error("An unexpected error occurred while sending {} (msgId={}). Exception trace:\n{}",
        				  MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(),
        				  Utils.getExceptionTrace(t, true));
        		registerSendFailure(messageUnit, "Unexpected error sending message", msgCtx.getFailureReason());
        	}
        }
    }

	/**
     * Sets the processing state of the given message unit to <i>FAILURE</i> and raises a event to indicate that the
     * sending of the message unit failed.
     *
     * @param messageUnit			to be sent
     * @param failureDescription	description of the failure
     * @param cause					cause of the failure
     * @throws StorageException		when the message unit's state could not be updated
     */
	private static void registerSendFailure(IMessageUnitEntity messageUnit, String failureDescription, Exception cause)
																						throws StorageException {
		// Set state to FAILURE for Signals or SUSPENDED for User Messages
    	HolodeckB2BCore.getStorageManager()
    				   .setProcessingState(messageUnit,
							messageUnit instanceof IUserMessage ? ProcessingState.SUSPENDED : ProcessingState.FAILURE,
							failureDescription);
    	// Raise event to signal this issue
    	HolodeckB2BCoreInterface.getEventProcessor().raiseEvent(
    			new GenericSendMessageFailure(messageUnit, failureDescription, cause));
	}


}

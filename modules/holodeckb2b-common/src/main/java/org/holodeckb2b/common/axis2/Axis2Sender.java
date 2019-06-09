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
package org.holodeckb2b.common.axis2;

import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;

import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
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
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.config.InternalConfiguration;
import org.holodeckb2b.common.constants.ProductId;
import org.holodeckb2b.common.handler.MessageProcessingContext;
import org.holodeckb2b.common.messagemodel.util.MessageUnitUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IPullRequest;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.entities.IErrorMessageEntity;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IPullRequestEntity;
import org.holodeckb2b.interfaces.persistency.entities.IReceiptEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;

/**
 * Is responsible for sending the message unit using the Axis2 framework. Depending on the messaging protocol the 
 * correct <i>Service</i> is chosen to ensure that the correct handlers are invoked. The <i>Service</i> to use is 
 * determined based on the value of the <b>PMode.MEPBinding</b> parameter. If its value starts with <i>
 * "http://holodeck-b2b.org/pmode/mepBinding/"</i> this class will look for a registered service with the name that
 * matches the last part of the MEPBinding, e.g. if the MEPBinding is ""http://holodeck-b2b.org/pmode/mepBinding/as2" it
 * looks for a Service named "as2". 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class Axis2Sender {
	private final static Logger log = LogManager.getLogger(Axis2Sender.class);
	
    /**
     * Sends the given message unit to the other MSH.
     *
     * @param messageUnit   The message unit to send
     */
    public static void sendMessage(final IMessageUnitEntity messageUnit) {
        OperationClient oc;
        final MessageContext msgCtx = new MessageContext();
        msgCtx.setFLOW(MessageContext.OUT_FLOW);

        log.trace("Check PMode.MEPBinding which Service should be used");
        IPMode pmode = HolodeckB2BCoreInterface.getPModeSet().get(messageUnit.getPModeId());
        if (pmode == null) {
        	log.error("Cannot send {} [msgId={}] because associated P-Mode {} is not available!", 
        				MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(), 
        				messageUnit.getPModeId());
        	return;
        }
        // The default protocol is AS4, so we will use the "as4" Service unless P-Mode indicates something else 
        String svcName = "as4";
        if (pmode.getMepBinding().startsWith("http://holodeck-b2b.org/pmode/mepBinding/"))  
        	svcName = pmode.getMepBinding().substring(pmode.getMepBinding().lastIndexOf('/'));
        
        ConfigurationContext configContext = ((InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration())
        																				.getAxisConfigurationContext();
        AxisConfiguration axisConfig = configContext.getAxisConfiguration();
        AxisService service;
        try {
			service = axisConfig.getService(svcName);
		} catch (AxisFault e) {
			service = null;
		}    
        if (service == null) {
        	log.error("Cannot send {} [msgId={}] because required {} Service is not installed!", 
    				   MessageUnitUtils.getMessageUnitName(messageUnit), messageUnit.getMessageId(), 
    				   messageUnit.getPModeId());
        	return;	
        }
        
        log.debug("Using {} Service to send {} [msgId={}]", svcName, MessageUnitUtils.getMessageUnitName(messageUnit), 
        			messageUnit.getMessageId());
        try {
	        AxisServiceGroup axisServiceGroup = service.getAxisServiceGroup();
	        ServiceGroupContext sgc = configContext.createServiceGroupContext(axisServiceGroup);
	        ServiceContext svcCtx = sgc.getServiceContext(service);
	            
	        // This dummy EPR has to be provided to be able to trigger message sending. It will be replaced later
	        // with the correct URL defined in the P-Mode
	        final EndpointReference targetEPR = new EndpointReference("http://holodeck-b2b.org/transport/dummy");
	        final Options options = new Options();
	        options.setTo(targetEPR);
	        options.setExceptionToBeThrownOnSOAPFault(false);
	        options.setProperty(HTTPConstants.USER_AGENT, ProductId.HTTP_HEADER);
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
		
	        final HttpClient httpClient = new HttpClient();
	        httpClient.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
	        msgCtx.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);

	        log.trace("Create an empty MessageProcessingContext for message with current configuration");
            final MessageProcessingContext procCtx = MessageProcessingContext.getFromMessageContext(msgCtx);
            if (messageUnit instanceof IUserMessage)
                procCtx.setUserMessage((IUserMessageEntity) messageUnit);
            else if (messageUnit instanceof IPullRequest)
            	procCtx.setPullRequest((IPullRequestEntity) messageUnit);
            else if (messageUnit instanceof IErrorMessage)
                procCtx.addSendingError((IErrorMessageEntity) messageUnit);
            else if (messageUnit instanceof IReceipt)
                procCtx.addSendingReceipt((IReceiptEntity) messageUnit);
            log.debug("Start send process for {} [msgId={}]", MessageUnitUtils.getMessageUnitName(messageUnit), 
        			  messageUnit.getMessageId());
            oc.execute(true);
        } catch (final Throwable t) {
            /* An error occurred while sending the message, it should however be already processed by one of the
               handlers. In that case the message context will not contain the failure reason. To prevent redundant
               logging we check if there is a failure reason before we log the error here.
            */
            final List<Throwable> errorStack = Utils.getCauses(t);
            final StringBuilder logMsg = new StringBuilder("\n\tError stack: ")
                                                    .append(errorStack.get(0).getClass().getSimpleName());
            for(int i = 1; i < errorStack.size(); i++) {
                logMsg.append("\n\t    Caused by: ").append(errorStack.get(i).getClass().getSimpleName());
            }
            logMsg.append(" {").append(errorStack.get(errorStack.size() - 1).getMessage()).append('}');
            log.error("An error occurred while sending the message [" + messageUnit.getMessageId() + "]!"
                     + logMsg.toString());
        }
    }
}

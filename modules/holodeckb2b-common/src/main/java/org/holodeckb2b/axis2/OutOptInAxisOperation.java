/**
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
package org.holodeckb2b.axis2;

import javax.xml.namespace.QName;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.ClientUtils;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.CallbackReceiver;
import org.apache.axis2.util.Utils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Is a special Axis2 {@link AxisOperation} implementation that supports the Out In MEP but does not require a response
 * message. This implementation is create because the default Axis2 implementation for the OutIn MEP throws an AxisFault
 * if no response is received. In an ebMS exchange however it can not be guaranteed that there is a response a replies
 * may be sent asynchronously.
 * <p>This class extends {@link OutInAxisOperation} to return a different {@link OperationClient} implementation.
 * Although there is just one method that changes in the <code>OperationClient</code> it must be copied from the super
 * class a an inner class can not be extended.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class OutOptInAxisOperation extends OutInAxisOperation {

    /**
     * Create a new instance of the OutOptInAxisOperation
     */
    public OutOptInAxisOperation(final QName name) {
        super(name);
        setMessageExchangePattern(WSDL2Constants.MEP_URI_OUT_IN);
    }

    /**
     * Returns the MEP client for an Out-IN operation that accepts an empty response. To use the client, you must call
     * addMessageContext() with a message context and then call execute() to execute the client.
     *
     * @param sc      The service context for this client to live within. Cannot be
     *                null.
     * @param options Options to use as defaults for this client. If any options are
     *                set specifically on the client then those override options
     *                here.
     */
    @Override
    public OperationClient createClient(final ServiceContext sc, final Options options) {
        return new OutOptInAxisOperationClient(this, sc, options);
    }

    /**
     * The client to handle the MEP. This is a copy of <code>OutInAxisOperationClient<code> inner class of {@link
     * OutInAxisOperation} with an adjusted {@link #handleResponse()} method.
     */
    class OutOptInAxisOperationClient extends OperationClient {

        private final Log log = LogFactory.getLog(OutOptInAxisOperationClient.class);

        OutOptInAxisOperationClient(final OutInAxisOperation axisOp, final ServiceContext sc, final Options options) {
            super(axisOp, sc, options);
        }

        /**
         * Adds message context to operation context, so that it will handle the logic correctly if the OperationContext
         * is null then new one will be created, and Operation Context will become null when some one calls reset().
         *
         * @param msgContext the MessageContext to add
         * @throws AxisFault
         */
        @Override
        public void addMessageContext(final MessageContext msgContext) throws AxisFault {
            msgContext.setServiceContext(sc);
            if (msgContext.getMessageID() == null) {
                setMessageID(msgContext);
            }
            axisOp.registerOperationContext(msgContext, oc);
        }

        /**
         * Returns the message context for a given message label.
         *
         * @param messageLabel : label of the message and that can be either "Out" or "In" and nothing else
         * @return Returns MessageContext.
         * @throws AxisFault
         */
        @Override
        public MessageContext getMessageContext(final String messageLabel)
                throws AxisFault {
            return oc.getMessageContext(messageLabel);
        }

        @Override
        public void setCallback(final Callback clbck) {
            throw new UnsupportedOperationException("Not supported, this method is deprecated!");
        }

        /**
         * Executes the MEP. What this does depends on the specific MEP client. The basic idea is to have the MEP client
         * execute and do something with the messages that have been added to it so far. For example, if its an Out-In
         * MEP, then if the Out message has been set, then executing the client asks it to send the message and get the
         * In message, possibly using a different thread.
         *
         * @param block Indicates whether execution should block or return ASAP. What block means is of course a
         * function of the specific MEP client. IGNORED BY THIS MEP CLIENT.
         * @throws AxisFault if something goes wrong during the execution of the MEP.
         */
        @Override
        public void executeImpl(final boolean block) throws AxisFault {
            if (log.isDebugEnabled()) {
                log.debug("Entry: OutOptInAxisOperationClient::execute, " + block);
            }
            if (completed) {
                throw new AxisFault(Messages.getMessage("mepiscomplted"));
            }
            final ConfigurationContext cc = sc.getConfigurationContext();

            // copy interesting info from options to message context.
            final MessageContext mc = oc.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            if (mc == null) {
                throw new AxisFault(Messages.getMessage("outmsgctxnull"));
            }
            prepareMessageContext(cc, mc);

            if (options.getTransportIn() == null && mc.getTransportIn() == null) {
                mc.setTransportIn(ClientUtils.inferInTransport(cc
                        .getAxisConfiguration(), options, mc));
            } else if (mc.getTransportIn() == null) {
                mc.setTransportIn(options.getTransportIn());
            }

            /**
             * If a module has set the USE_ASYNC_OPERATIONS option then we override the behaviour for sync calls, and
             * effectively USE_CUSTOM_LISTENER too. However we leave real async calls alone.
             */
            boolean useAsync = false;
            if (!mc.getOptions().isUseSeparateListener()) {
                final Boolean useAsyncOption
                        = (Boolean) mc.getProperty(Constants.Configuration.USE_ASYNC_OPERATIONS);
                if (log.isDebugEnabled()) {
                    log.debug("OutInAxisOperationClient: useAsyncOption " + useAsyncOption);
                }
                if (useAsyncOption != null) {
                    useAsync = useAsyncOption.booleanValue();
                }
            }

            final EndpointReference replyTo = mc.getReplyTo();
            if (replyTo != null) {
                if (replyTo.isWSAddressingAnonymous()
                        && replyTo.getAllReferenceParameters() != null) {
                    mc.setProperty(AddressingConstants.INCLUDE_OPTIONAL_HEADERS, Boolean.TRUE);
                }

                final String customReplyTo = (String) options.getProperty(Options.CUSTOM_REPLYTO_ADDRESS);
                if (!(Options.CUSTOM_REPLYTO_ADDRESS_TRUE.equals(customReplyTo))) {
                    if (!replyTo.hasAnonymousAddress()) {
                        useAsync = true;
                    }
                }
            }

            if (useAsync || mc.getOptions().isUseSeparateListener()) {
                sendAsync(useAsync, mc);
            } else {
                if (block) {
                    // Send the SOAP Message and receive a response
                    send(mc);
                    completed = true;
                } else {
                    sc.getConfigurationContext().getThreadPool().execute(
                            new NonBlockingInvocationWorker(callback, mc, axisCallback));
                }
            }
        }

        private void sendAsync(final boolean useAsync, final MessageContext mc)
                throws AxisFault {
            if (log.isDebugEnabled()) {
                log.debug("useAsync=" + useAsync + ", seperateListener="
                        + mc.getOptions().isUseSeparateListener());
            }
            /**
             * We are following the async path. If the user hasn't set a callback object then we must block until the
             * whole MEP is complete, as they have no other way to get their reply message.
             */
        // THREADSAFE issue: Multiple threads could be trying to initialize the callback receiver
            // so it is synchronized.  It is not done within the else clause to avoid the
            // double-checked lock antipattern.
            CallbackReceiver callbackReceiver;
            synchronized (axisOp) {
                if (axisOp.getMessageReceiver() != null
                        && axisOp.getMessageReceiver() instanceof CallbackReceiver) {
                    callbackReceiver = (CallbackReceiver) axisOp.getMessageReceiver();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating new callback receiver");
                    }
                    callbackReceiver = new CallbackReceiver();
                    axisOp.setMessageReceiver(callbackReceiver);
                    if (log.isDebugEnabled()) {
                        log.debug("OutInAxisOperation: callbackReceiver " + callbackReceiver + " : " + axisOp);
                    }
                }
            }

            SyncCallBack internalCallback = null;
            if (callback != null) {
                callbackReceiver.addCallback(mc.getMessageID(), callback);
                if (log.isDebugEnabled()) {
                    log.debug("OutInAxisOperationClient: Creating callback");
                }
            } else if (axisCallback != null) {
                callbackReceiver.addCallback(mc.getMessageID(), axisCallback);
                if (log.isDebugEnabled()) {
                    log.debug("OutInAxisOperationClient: Creating axis callback");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Creating internal callback");
                }
                internalCallback = new SyncCallBack();
                callbackReceiver.addCallback(mc.getMessageID(), internalCallback);
                if (log.isDebugEnabled()) {
                    log.debug("OutInAxisOperationClient: Creating internal callback");
                }
            }

            /**
             * If USE_CUSTOM_LISTENER is set to 'true' the replyTo value will not be replaced and Axis2 will not start
             * its internal listner. Some other enntity (e.g. a module) should take care of obtaining the response
             * message.
             */
            Boolean useCustomListener
                    = (Boolean) options.getProperty(Constants.Configuration.USE_CUSTOM_LISTENER);
            if (useAsync) {
                useCustomListener = Boolean.TRUE;
            }
            if (useCustomListener == null || !useCustomListener.booleanValue()) {
                final EndpointReference replyTo = mc.getReplyTo();
                if (replyTo == null || replyTo.hasAnonymousAddress()) {
                    final EndpointReference replyToFromTransport
                            = mc.getConfigurationContext().getListenerManager().
                            getEPRforService(sc.getAxisService().getName(),
                                    axisOp.getName().getLocalPart(), mc
                                    .getTransportIn().getName());

                    if (replyTo == null) {
                        mc.setReplyTo(replyToFromTransport);
                    } else {
                        replyTo.setAddress(replyToFromTransport.getAddress());
                    }
                }
            }

            //if we don't do this , this guy will wait till it gets HTTP 202 in the HTTP case
            mc.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, Boolean.TRUE);
            mc.getConfigurationContext().registerOperationContext(mc.getMessageID(), oc);
            AxisEngine.send(mc);
            if (internalCallback != null) {
                internalCallback.waitForCompletion(options.getTimeOutInMilliSeconds());

                // process the result of the invocation
                if (internalCallback.envelope == null) {
                    if (internalCallback.error == null) {
                        log.error("Callback had neither error nor response");
                    }
                    if (options.isExceptionToBeThrownOnSOAPFault()) {
                        throw AxisFault.makeFault(internalCallback.error);
                    }
                }
            }
        }

        /**
         * When synchronous send() gets back a response MessageContext, this is the workhorse method which processes it.
         *
         * @param responseMessageContext the active response MessageContext
         * @throws AxisFault if something went wrong
         */
        protected void handleResponse(final MessageContext responseMessageContext) throws AxisFault {
        // Options object reused above so soapAction needs to be removed so
            // that soapAction+wsa:Action on response don't conflict
            responseMessageContext.setSoapAction(null);

            if (responseMessageContext.getEnvelope() == null) {
                try {
                    final SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(responseMessageContext);
                    if (resenvelope != null)
                        responseMessageContext.setEnvelope(resenvelope);
                } catch (final AxisFault af) {
                    // This AxisFault indicates that there was no response received. Because this is allowd in ebMS
                    // exchanges we just ignore this.
                }

            }
            SOAPEnvelope resenvelope = responseMessageContext.getEnvelope();
            if (resenvelope != null) {
                AxisEngine.receive(responseMessageContext);
                if (responseMessageContext.getReplyTo() != null) {
                    sc.setTargetEPR(responseMessageContext.getReplyTo());
                }

            // rampart handlers change the envelope and set the decrypted envelope
                // so need to check the new one else resenvelope.hasFault() become false.
                resenvelope = responseMessageContext.getEnvelope();
                if (resenvelope.hasFault() || responseMessageContext.isProcessingFault()) {
                    if (options.isExceptionToBeThrownOnSOAPFault()) {
                        // does the SOAPFault has a detail element for Excpetion
                        throw Utils.getInboundFaultFromMessageContext(responseMessageContext);
                    }
                }
            }
        }

        /**
         * Synchronously send the request and receive a response. This relies on the transport correctly connecting the
         * response InputStream!
         *
         * @param msgContext the request MessageContext to send.
         * @return Returns MessageContext.
         * @throws AxisFault Sends the message using a two way transport and waits for a response
         */
        protected MessageContext send(final MessageContext msgContext) throws AxisFault {

        // create the responseMessageContext
            final MessageContext responseMessageContext
                    = msgContext.getConfigurationContext().createMessageContext();

            responseMessageContext.setServerSide(false);
            responseMessageContext.setOperationContext(msgContext.getOperationContext());
            responseMessageContext.setOptions(new Options(options));
            responseMessageContext.setMessageID(msgContext.getMessageID());
            addMessageContext(responseMessageContext);
            responseMessageContext.setServiceContext(msgContext.getServiceContext());
            responseMessageContext.setAxisMessage(
                    axisOp.getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));

            //sending the message
            AxisEngine.send(msgContext);

            responseMessageContext.setDoingREST(msgContext.isDoingREST());

        // Copy RESPONSE properties which the transport set onto the request message context when it processed
            // the incoming response recieved in reply to an outgoing request.
            responseMessageContext.setProperty(MessageContext.TRANSPORT_HEADERS,
                    msgContext.getProperty(MessageContext.TRANSPORT_HEADERS));
            responseMessageContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE,
                    msgContext.getProperty(HTTPConstants.MC_HTTP_STATUS_CODE));

            responseMessageContext.setProperty(MessageContext.TRANSPORT_IN, msgContext
                    .getProperty(MessageContext.TRANSPORT_IN));
            responseMessageContext.setTransportIn(msgContext.getTransportIn());
            responseMessageContext.setTransportOut(msgContext.getTransportOut());
            handleResponse(responseMessageContext);
            return responseMessageContext;
        }

        /**
         * This class is the workhorse for a non-blocking invocation that uses a two way transport.
         */
        private class NonBlockingInvocationWorker implements Runnable {

            private final Callback callback;

            private final MessageContext msgctx;
            private final AxisCallback axisCallback;

            public NonBlockingInvocationWorker(final Callback callback,
                    final MessageContext msgctx,
                    final AxisCallback axisCallback) {
                this.callback = callback;
                this.msgctx = msgctx;
                this.axisCallback = axisCallback;
            }

            public void run() {
                try {
                    // send the request and wait for response
                    final MessageContext response = send(msgctx);
                    // call the callback
                    if (response != null) {
                        final SOAPEnvelope resenvelope = response.getEnvelope();

                        if (resenvelope.hasFault()) {
                            final SOAPBody body = resenvelope.getBody();
                        // If a fault was found, create an AxisFault with a MessageContext so that
                            // other programming models can deserialize the fault to an alternative form.
                            final AxisFault fault = new AxisFault(body.getFault(), response);
                            if (callback != null) {
                                callback.onError(fault);
                            } else if (axisCallback != null) {
                                if (options.isExceptionToBeThrownOnSOAPFault()) {
                                    axisCallback.onError(fault);
                                } else {
                                    axisCallback.onFault(response);
                                }
                            }

                        } else {
                            if (callback != null) {
                                final AsyncResult asyncResult = new AsyncResult(response);
                                callback.onComplete(asyncResult);
                            } else if (axisCallback != null) {
                                axisCallback.onMessage(response);
                            }

                        }
                    }

                } catch (final Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    } else if (axisCallback != null) {
                        axisCallback.onError(e);
                    }

                } finally {
                    if (callback != null) {
                        callback.setComplete(true);
                    } else if (axisCallback != null) {
                        axisCallback.onComplete();
                    }
                }
            }
        }

        /**
         * This class acts as a callback that allows users to wait on the result.
         */
        private class SyncCallBack implements AxisCallback {

            boolean complete;
            boolean receivedFault;

            public boolean waitForCompletion(final long timeout) throws AxisFault {
                synchronized (this) {
                    try {
                        if (complete) {
                            return !receivedFault;
                        }
                        wait(timeout);
                        if (!complete) {
                            // We timed out!
                            throw new AxisFault(Messages.getMessage("responseTimeOut"));
                        }
                    } catch (final InterruptedException e) {
                        // Something interrupted our wait!
                        error = e;
                    }
                }

                if (error != null) {
                    throw AxisFault.makeFault(error);
                }

                return !receivedFault;
            }

            /**
             * This is called when we receive a message.
             *
             * @param msgContext the (response) MessageContext
             */
            public void onMessage(final MessageContext msgContext) {
            // Transport input stream gets closed after calling setComplete
                // method. Have to build the whole envelope including the
                // attachments at this stage. Data might get lost if the input
                // stream gets closed before building the whole envelope.

                // TODO: Shouldn't need to do this - need to hook up stream closure to Axiom completion
                this.envelope = msgContext.getEnvelope();
                this.envelope.buildWithAttachments();
            }

            /**
             * This gets called when a fault message is received.
             *
             * @param msgContext the MessageContext containing the fault.
             */
            public void onFault(final MessageContext msgContext) {
                error = Utils.getInboundFaultFromMessageContext(msgContext);
            }

            /**
             * This is called at the end of the MEP no matter what happens, quite like a finally block.
             */
            public synchronized void onComplete() {
                complete = true;
                notify();
            }

            private SOAPEnvelope envelope;

            private Exception error;

            public void onError(final Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Entry: OutInAxisOperationClient$SyncCallBack::onError, " + e);
                }
                error = e;
                if (log.isDebugEnabled()) {
                    log.debug("Exit: OutInAxisOperationClient$SyncCallBack::onError");
                }
            }
        }
    }
}


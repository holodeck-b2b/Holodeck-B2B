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



import java.util.List;

import org.holodeckb2b.interfaces.config.IConfiguration;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;
import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEventConfiguration;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;

/**
 * Represents a P-Mode that governs the exchange of messages.
 * <p>The P-Mode concept is described in the chapter 4 and appendix D of the ebMS v3 Core Specification. The P-Mode
 * contains the settings needed to successfully exchange message with another MSH. The specification only defines an
 * abstract model for the parameters, the actual P-Mode definition is left up to implementations. This interface defines
 * the structure of the Holodeck B2B P-Modes.
 * <p>This interfaces model is based on the structure described in appendix D of the Core Specification. As described
 * in section D.2 one leg may involve two sets of P-Mode parameters if pulling is used as the pull request signal has
 * its own set of parameters. This is reflected in this model by the <i>flow</i> concept. When a leg uses the pull
 * binding it has two flows, one for the pull request and one for the user message.
 * <p>The P-Mode parameters are used to set values in the ebMS message header and determine how and where to send
 * messages. All parameter values must be known when the message is sent, but it is not necessary to include them all
 * in the P-Mode. This would require a P-Mode for each exchanged message while some message only differ on some
 * configuration parameters. Therefor Holodeck B2B allows the P-Mode to define only the common parameters and supply the
 * message specific ones when the message is submitted.
 * <p><b>NOTE 1: </b>Although the focus of Holodeck B2B is on implementation of ebMS V3/AS4 messaging protocol P-Modes
 * are also used to configure other messaging protocols like AS2 and ebMS2. However some parameters will need to be
 * mapped to protocol specific settings. Such mappings should be documented in the project providing the support for the
 * specific messaging protocol.
 * <p><b>NOTE 2: </b>The current version does not contain all P-Mode parameters described by the Core Specification and
 * AS4 profile. When new functionality is implemented parameters will be added to this interface.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IPMode {

    /**
     * Gets the P-Mode id.
     * <p>Although <code>id</code> is defined as optional by the ebMS Core Specification in this model it is defined as
     * a REQUIRED parameter. This is done as the <code>id</code> is used as the key for the P-Mode within the set of
     * deployed P-Modes.
     *
     * @return      A <b>non empty</b> String that uniquely identifies this P-Mode within the set of deployed P-Modes.
     */
    String getId();

    /**
     * Returns whether the P-Mode id should be included in user messages.
     * <p>As defined in the ebMS V3 Core Specification the P-Mode id is an optional information item in the ebMS message
     * header. Because the MSHs involved in a message exchange may use different identifiers for the P-Mode that governs
     * the exchange, inclusion is optional to avoid confusion among MSHs.
     * <p>If not configured Holodeck B2B will not include the P-Mode id or expect it to be included in the message.
     * <p>Note that the P-Mode id is included as a child of the agreement reference and including the P-Mode id also
     * requires inclusion of an agreement reference.
     *
     * @return      <code>Boolean.TRUE</code> when the P-Mode id should be included in the message,<br>
     *              <code>null</code> or <code>Boolean.FALSE</code> when the P-Mode id should be NOT included in
     *              the message
     */
    Boolean includeId();

    /**
     * Gets the meta-data on the <i>business level</i> agreement that governs the message exchange configured by this
     * P-Mode. This information is for use by the applications processing the user messages, the P-Mode is the
     * <i>technical</i> agreement between two MSHs on how to process messages.
     * <p>The information on the agreement is optional but when the P-Mode id should be included in the message it MUST
     * be supplied!
     *
     * @return An {@link IAgreement} object containing the meta-data on the (business level) agreement that governs this
     *         message exchange, or<br>
     *         <code>null</code> if information on the business agreement is not specified.
     */
    IAgreement getAgreement();

    /**
     * Gets the message exchange pattern (MEP) used by this P-Mode.
     * <p>NOTE: Holodeck B2B currently only support the One-Way MEP!
     *
     * @return  The URI defined in the Core Specification that defines the MEP used by this P-Mode.
     * @see EbMSConstants#ONE_WAY_MEP
     * @see EbMSConstants#TWO_WAY_MEP
     */
    String getMep();

    /**
     * Gets the MEP binding used by the P-Mode.
     *
     * @return  URI defining the messaging protocol used. This MUST be an URI defined in the ebMS V3 Core Specification
     * 			when the ebMS V3 message exchange protocol is used.
     * @see EbMSConstants#ONE_WAY_PULL
     * @see EbMSConstants#ONE_WAY_PUSH
     * @see EbMSConstants#TWO_WAY_PUSH_PUSH
     * @see EbMSConstants#TWO_WAY_PUSH_PULL
     * @see EbMSConstants#TWO_WAY_PULL_PUSH
     */
    String getMepBinding();

    /**
     * Gets information on the trading partner that initiates the execution of the message exchange.
     * <p>This the partner that sends the first ebMS message in an exchange. Note that this not necessarily the
     * <i>Sender</i> of the first user message because in a pull binding the first message is a pull request and the
     * initiating partner is the <i>Receiver</i> of the user message. The <i>Initiator</i> and <i>Responder</i> do not
     * change during the execution of a MEP.
     *
     * @return  An {@link ITradingPartnerConfiguration} object containing the configuration for the initiator of this
     *          message exchange, or<br>
     *          <code>null</code> if this information is not specified by the P-Mode. In this case the trading partner
     *          information must be supplied when the user message is submitted.
     */
    ITradingPartnerConfiguration getInitiator();

    /**
     * Gets information on the trading partner that responds to the initial message.
     * <p>The <i>Responder</i> is not necessarily the <i>Receiving</i> partner in the first leg because in a pull the
     * <i>Responder</i> sends the user message in reply to the pull request. The <i>Initiator</i> and <i>Responder</i>
     * do not change during the execution of a MEP.
     *
     * @return  An {@link ITradingPartnerConfiguration} object containing the configuration for the responder of this
     *          message exchange, or<br>
     *          <code>null</code> if this information is not specified by the P-Mode. In this case the trading partner
     *          information must be supplied when the user message is submitted.
     */
    ITradingPartnerConfiguration getResponder();

    /**
     * Gets the configuration of the legs that are part of the message exchange governed by this P-Mode.
     * <p>A leg describes the exchange of a <b>user message</b>, so the number of legs only depends on the MEP
     * (One- vs Two-Way) and not the MEP binding which may require multiple message exchanges.
     *
     * @return One or two, depending on the MEP, {@link ILeg} objects containing the configuration of the legs.
     */
    List<? extends ILeg> getLegs();

    /**
     * Gets the configuration of the leg with the specified label within the P-Mode.
     * <p>Although the leg's label is only relevant for P-Modes that manage a Two-Way MEP this method is also used by
     * the Holodeck B2B Core to get the single leg of One-Way P-Modes. In that case either no specific label or the
     * <i>request</i> label may be requested depending on if the P-Mode implementation assigns a label to the leg of a
     * One-Way P-Mode (i.e. only when the implementation does not assign a label Holodeck B2B will not provide one when
     * calling this method).
     *
     * @param label     The label of the leg to get the configuration of
     * @return          A {@link ILeg} object containing the configuration of the leg
     */
    ILeg getLeg(Label label);

    /**
     * Gets the setting for whether Holodeck B2B should perform a strict validation of the ebMS header meta-data
     * as specified in the ebMS Specifications for messages processed under this P-Mode.
     * <p>For Holodeck B2B to be able to process a message unit it does not need to conform to all the requirements as
     * stated in the ebMS Specifications, for example the formatting of values is mostly irrelevant to Holodeck B2B.
     * Therefore two validation modes are offered, <i>basic</i> and <i>strict</i>.
     * <p>Note that there is also a global setting for the validation mode ({@link
     * IConfiguration#useStrictHeaderValidation()}. This P-Mode setting can only be used to make the validation more
     * strict, not more relaxed, i.e. if the global setting is to use strict validation the P-Mode setting is ignored.
     *
     * @return <code>true</code> if a strict validation of the ebMS header meta-data should be performed for message
     *         units which processing is governed by this P-Mode,<br>
     *         <code>false</code> if a basic validation is enough
     * @since 4.0.0
     */
    boolean useStrictHeaderValidation();

    /**
     * Returns the configuration for handling <i>"events"</i> that occur during the processing of message units that are
     * governed by this P-Mode. These <i>message processing events</i> are used to provide additional information to the
     * business application about the processing of a message unit in addition to the formally specified <i>Submit</i>,
     * <i>Deliver</i> and <i>Notify</i> operations. An example of an event is that a message unit has been (re)sent.
     * <p>NOTE: The Holodeck B2B event processor will first check the Leg configuration if there are events handlers
     * configured and executed these. Only if there are no handlers configured at Leg level or processing should
     * continue the handler configuration at the P-Mode level is evaluated.
     *
     * @return A {@link List} of {@link IMessageProcessingEventConfiguration}s that specify which event handlers should
     *         be used for events that occur while processing message units of this P-Mode.
     * @see IMessageProcessingEvent
     * @see org.holodeckb2b.interfaces.eventprocessing
     * @since 7.0.0
     */
    List<IMessageProcessingEventConfiguration> getMessageProcessingEventConfiguration();

}

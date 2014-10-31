/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import java.util.Collection;
import org.holodeckb2b.common.general.Constants;
import org.holodeckb2b.common.general.IAgreement;
import org.holodeckb2b.common.general.ITradingPartner;

/**
 * Represents a P-Mode that governs the exchange of messages. The P-Mode concept is described in the ebMS v3 Core 
 * Specification, see chapter 4 and appendix D for more information. 
 * <p>This interfaces model is based on the structure described in appendix D of the Core Specification. As described
 * in section D.2 one leg may involve two sets of P-Mode parameters if pulling is used as the pull request signal has
 * its own set of parameters. This is reflected in this model by the <i>flow</i> concept. When a leg uses the pull
 * binding it has two flows, one for the pull request and one for the user message.
 * <p>The P-Mode parameters are used to set values in the ebMS message header and determine how and where to send 
 * messages. All parameter values must be known when the message is sent, but it is not necessary to include them all
 * in the P-Mode. This would require a P-Mode for each exchanged message while some message only differ on some 
 * configuration parameters. Therefor Holodeck B2B allows the P-Mode to define the common parameters and supply the
 * message specific ones when the message is submitted.
 * <p><b>NOTE 1: </b>Although the P-Mode model used here does support Two-Way MEP bindings, Holodeck B2B currently only
 * supports One-Way MEP bindings (support for One-Way bindings allows for full support of the AS4 profile).
 * <p><b>NOTE 2: </b>The current version does not contain all P-Mode parameters described by the Core Specification and
 * AS4 profile. When new functionality is implemented parameters will be added to this interface.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IPMode {
   
    /**
     * Gets the P-Mode id. Although <code>id</code> is defined as optional by the ebMS Core Specification in this model 
     * it is defined as a REQUIRED parameter. This is done as the <code>id</code> is used as the key for the P-Mode 
     * within the set of deployed P-Modes.  
     * 
     * @return      A <b>non empty</b> String that uniquely identifies this P-Mode within the set of deployed P-Modes.
     */
    public String getId();
    
    /**
     * Returns whether the P-Mode id should be included in user messages. As defined in the ebMS V3 Core Specification 
     * the P-Mode id is an optional information item in the header. Because the MSHs involved in the message exchange
     * may use different identifiers for the same P-Mode inclusion is optional to avoid confusion among MSHs.
     * <p>Note that the P-Mode id is included as a child of the agreement reference and including the P-Mode id also 
     * requires inclusion of an agreement reference. 
     * 
     * @return      <code>Boolean.TRUE</code> when the P-Mode id should be included in the message,<br>
     *              <code>Boolean.FALSE</code> when the P-Mode id should be NOT included in the message, or<br>
     *              <code>null</code> if not configured (in which case Holodeck B2B uses true as default)
     */
    public Boolean includeId();
    
    /**
     * Gets the meta-data of <i>business level</i> agreement that governs the message exchanged configured by this 
     * P-Mode. This information is for use by the applications processing the user messages, the P-Mode is the 
     * <i>technical</i> agreement between two MSHs on how to process messages.
     * 
     * @return An {@link IAgreement} object containing the meta-data on the (business level) agreement that governs this
     *         message exchange, or<br>
     *         <code>null</code> if information on the business agreement is not specified.
     */
    public IAgreement getAgreement();
    
    /**
     * Gets the MEP used by this P-Mode.
     * 
     * @return  The URI defined in the Core Specification that defines the MEP used by this P-Mode.
     * @see Constants#ONE_WAY_MEP
     * @see Constants#TWO_WAY_MEP
     */
    public String getMep();
    
    /**
     * Gets the MEP binding used by the P-Mode.
     * 
     * @return  The URI defined in the Core Specification that defines the MEP binding used by this P-Mode.
     * @see Constants#ONE_WAY_PULL
     * @see Constants#ONE_WAY_PUSH
     * @see Constants#TWO_WAY_PUSH_PUSH
     * @see Constants#TWO_WAY_PUSH_PULL
     * @see Constants#TWO_WAY_PULL_PUSH
     */
    public String getMepBinding();

    /**
     * Gets information on the trading partner that initiates the execution of the message exchange. This the partner
     * that sends the first ebMS message but is not necessarily acting in the <i>Sending</i> role because in a pull 
     * binding the first message is a pull request and the initiating partner is the receiver of the user message.
     * 
     * @return  An {@link ITradingPartner} object identifying the initiator of this message exchange, or<br>
     *          <code>null</code> if this information is not specified by the P-Mode. In this case the trading partner
     *          information must be supplied when the user message is submitted.
     */
    public ITradingPartner getInitiator();
    
    /**
     * Gets information on the trading partner that responds to the initial message. Note that this is not necessarily 
     * the partner acting in the <i>Receiving</i> role because in a pull binding it may be responding to a pull request
     * with an user message and therefore be acting as <i>Sender</i>.
     * 
     * @return  An {@link ITradingPartner} object identifying the responder of this message exchange, or<br>
     *          <code>null</code> if this information is not specified by the P-Mode. In this case the trading partner
     *          information must be supplied when the user message is submitted.
     */
    public ITradingPartner getResponder();
    
    /**
     * Gets the configuration of the legs that are part of the message exchange governed by this P-Mode. A leg describes
     * the exchange of a <b>user message</b>, so the number of legs only depends on the MEP (One- vs Two-Way) and not 
     * the MEP binding which may require multiple message exchanges.
     * 
     * @return One or two, depending on the MEP, {@link ILeg} objects containing the configuration of the legs.
     */
    public Collection<ILeg> getLegs();
}

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
package org.holodeckb2b.ebms3.submit.core;

import java.util.Collection;
import java.util.Iterator;

import org.holodeckb2b.common.messagemodel.util.CompareUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.mmd.xml.AgreementReference;
import org.holodeckb2b.ebms3.mmd.xml.CollaborationInfo;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.mmd.xml.Service;
import org.holodeckb2b.ebms3.mmd.xml.TradingPartner;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.pmode.IBusinessInfo;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.submit.MessageSubmitException;

/**
 * Is a helper class to create a complete set of configuration parameters that define how a submitted user message must
 * be processed. It takes the set of parameters from the message submission and combines this with the set of parameters
 * from the P-Mode that governs the message processing.
 * <p>When both sets (submitted meta-data and P-Mode) contain the same parameter they must be equal in order to form
 * valid unified set. If a conflict is detected this is signaled by throwing a {@link MessageSubmitException}. An
 * exception to this rule are the message properties, they are just merged (=union of properties defined in MMD and
 * PMode) with properties from the MMD taking precedence over PMode when defined in both sets.
 * <p>This class also checks for completeness of the unified set, i.e. the resulting set of message meta data must be
 * enough to process the message. See method documentation for more information on completeness requirements.
 * <p>Because the input of a message submit is an implementation of the {@link IUserMessage} it can not be extended with
 * parameters from the P-Mode. Therefor this class uses an instance of {@link MessageMetaData} to create the complete
 * set of meta-data.
 * <p>This class is to be used only by the internal message submitter, therefore its visibility is limited to package.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
final class MMDCompleter {

    /**
     * The P-Mode that must be used to complete the MMD
     */
    private final IPMode       pmode;
    /**
     * The P-Mode parameters for the Leg the message is exchanged on
     */
    private final ILeg         leg;
    /**
     * The unified set of message meta data
     */
    private final MessageMetaData cd;

    /**
     * Combines the meta-data information on a user message obtained during submission and from the P-Mode to one set
     * of parameters to be used for processing the message.
     * <p>When both sets (submitted meta-data and P-Mode) contain the same parameter they must be equal in order to form
     * valid unified set.
     *
     * @param submittedMMD      The submitted message meta-data
     * @param pmode             The found P-Mode based on the given message meta-data
     * @return                  The combined set of meta-data to be used for further processing
     * @throws MessageSubmitException   When the unified set does not contain all required meta-data required for
     *                                  sending the message, or<br>
     *                                  when a parameter exists in both sets but with a different value.
     */
    public static IUserMessage complete(final IUserMessage submittedMMD, final IPMode pmode) throws MessageSubmitException {
        final MMDCompleter completer = new MMDCompleter(submittedMMD, pmode);

        return completer.complete();
    }

    /**
     * Completes the message meta data by combining the information from the submission and the P-Mode. When both
     * the submission and P-Mode contain the same parameters it is checked that these do not conflict (for most
     * parameters this means that they must be equal).
     *
     * @return  A complete set of message meta-data
     * @throws MessageSubmitException   When the unified set does not contain all required meta-data required for
     *                                  sending the message, or<br>
     *                                  when a parameter exists in both sets but with a different value.
     */
    private IUserMessage complete() throws MessageSubmitException {

        final String pmMPC = (leg.getUserMessageFlow() != null && leg.getUserMessageFlow().getBusinessInfo() != null ?
                            leg.getUserMessageFlow().getBusinessInfo().getMpc() : null);
        switch (Utils.compareStrings(cd.getMPC(), pmMPC)) {
            case -2 :
                throw new MessageSubmitException("Different MPC values (submitted: " + cd.getMPC() + ",P-Mode: " + pmMPC
                                              + ") specified!");
            case 2 :
                cd.setMPC(pmMPC);
        }

        completeSender();
        completeReceiver();
        completeCollaborationInfo();
        completeProperties();

        return cd;
    }

    /**
     * Completes the information on the sender of this message. This information is used to fill the
     * <code>eb:From</code> element in the header. This is a required element that must be provided by either submitted
     * meta-data or P-Mode. This method checks a complete {@link ITradingPartner} object, not its individual fields.
     *
     * @throws MessageSubmitException When sender information is missing or incomplete in both submitted meta-data and
     *                                P-Mode or when the sender information in submitted meta-data and P-Mode conflicts
     */
    private void completeSender() throws MessageSubmitException {
        // Get sender info from MMD
        final ITradingPartner ms = cd.getSender();
        Collection<IPartyId> sPartyIds = null;
        String  sRole = null;
        if (ms != null) {
            sPartyIds = ms.getPartyIds();
            sRole = ms.getRole();
        }
        // Get sender info from P-Mode
        ITradingPartner ps = null;
        // When pulling is used the responder is sending the message!
        if (pmode.getMepBinding().equals(EbMSConstants.ONE_WAY_PUSH))
            ps = pmode.getInitiator();
        else
            ps = pmode.getResponder();
        Collection<IPartyId> pmPartyIds = null;
        String  pmRole = null;
        if (ps != null) {
            pmPartyIds = ps.getPartyIds();
            pmRole = ps.getRole();
        }

        // Check PartyId(s)
        if (Utils.isNullOrEmpty(sPartyIds) && Utils.isNullOrEmpty(pmPartyIds))
            throw new MessageSubmitException("Missing PartyId information for Sender of the message!");
        else if (Utils.isNullOrEmpty(sPartyIds))
            // PartyId(s) specified in P-Mode
            cd.setSender(ps);
        else if (!Utils.isNullOrEmpty(pmPartyIds))
            // Both submission and P-Mode specify PartyId(s) => must be equal
            if (!CompareUtils.areEqual(sPartyIds, pmPartyIds))
                throw new MessageSubmitException("PartyId(s) for Sender in submission differ from ones in selected P-Mode!");
        // else // Only submission contained PartyId(s), already included

        // Check Role
        if (Utils.isNullOrEmpty(sRole) && Utils.isNullOrEmpty(pmRole))
            throw new MessageSubmitException("Missing Role information for Sender of the message!");
        else if (Utils.isNullOrEmpty(sRole))
            // Role specified in P-Mode
            ((TradingPartner) cd.getSender()).setRole(pmRole);
        else if (!Utils.isNullOrEmpty(pmRole)) {
            // Both submission and P-Mode specify Role => must be equal
            if (!pmRole.equals(sRole))
                throw new MessageSubmitException("Role of Sender in submission differs from one in selected P-Mode!");
        } else
            // Only submission contains Role, but since sender info may be overriden because P-Mode specified the
            // PartyId(s), set it again
            ((TradingPartner) cd.getSender()).setRole(sRole);
    }

    /**
     * Completes the information on the receiver of this message. This information is used to fill the
     * <code>eb:To</code> element in the header. This is a required element that must be provided by either submitted
     * meta-data or P-Mode. This method checks a complete {@link ITradingPartner} object, not its individual fields.
     *
     * @throws MessageSubmitException When receiver information is missing or incomplete in both submitted meta-data and
     *                                P-Mode or when the receiver information in submitted meta-data and P-Mode
     *                                conflicts
     */
    private void completeReceiver() throws MessageSubmitException {
        // Get receiver info from MMD
        final ITradingPartner mr = cd.getReceiver();
        Collection<IPartyId> sPartyIds = null;
        String  sRole = null;
        if (mr != null) {
            sPartyIds = mr.getPartyIds();
            sRole = mr.getRole();
        }
        // Get receiver info from P-Mode
        ITradingPartner pr = null;
        // When pulling is used the intiator is receiving the message!
        if (pmode.getMepBinding().equals(EbMSConstants.ONE_WAY_PUSH))
            pr = pmode.getResponder();
        else
            pr = pmode.getInitiator();
        Collection<IPartyId> pmPartyIds = null;
        String  pmRole = null;
        if (pr != null) {
            pmPartyIds = pr.getPartyIds();
            pmRole = pr.getRole();
        }

        // Check PartyId(s)
        if (Utils.isNullOrEmpty(sPartyIds) && Utils.isNullOrEmpty(pmPartyIds))
            throw new MessageSubmitException("Missing PartyId information for Receiver of the message!");
        else if (Utils.isNullOrEmpty(sPartyIds))
            // PartyId(s) specified in P-Mode
            cd.setReceiver(pr);
        else if (!Utils.isNullOrEmpty(pmPartyIds))
            // Both submission and P-Mode specify PartyId(s) => must be equal
            if (!CompareUtils.areEqual(sPartyIds, pmPartyIds))
                throw new MessageSubmitException("PartyId(s) for Receiver in submission differ from ones in selected P-Mode!");
        // else // Only submission contained PartyId(s), already included

        // Check Role
        if (Utils.isNullOrEmpty(sRole) && Utils.isNullOrEmpty(pmRole))
            throw new MessageSubmitException("Missing Role information for Receiver of the message!");
        else if (Utils.isNullOrEmpty(sRole))
            // Role specified in P-Mode
            ((TradingPartner) cd.getReceiver()).setRole(pmRole);
        else if (!Utils.isNullOrEmpty(pmRole)) {
            // Both submission and P-Mode specify Role => must be equal
            if (!pmRole.equals(sRole))
                throw new MessageSubmitException("Role for Receiver in submission differs from one in selected P-Mode!");
        } else
            // Only submission contains Role, but since sender info may be overriden because P-Mode specified the
            // PartyId(s), set it again
            ((TradingPartner) cd.getReceiver()).setRole(sRole);
    }

    /**
     * Completes the collaboration information om the message. Used to fill the <code>eb:CollaborationInfo</code>
     * element in the ebMS header. Must contain at least service, action and conversation id information, agreement
     * reference is optional.
     *
     * @throws MessageSubmitException When collaboration information is missing or incomplete in both submitted
     *                                meta-data and P-Mode or when the receiver information in submitted meta-data and
     *                                P-Mode conflicts
     */
    private void completeCollaborationInfo() throws MessageSubmitException {
        final CollaborationInfo sci = (CollaborationInfo) cd.getCollaborationInfo();
        final IBusinessInfo pbi = (leg.getUserMessageFlow() != null ? leg.getUserMessageFlow().getBusinessInfo() : null);

        // The submission must include a ConversationId
        if (sci == null || Utils.isNullOrEmpty(sci.getConversationId()))
            throw new MessageSubmitException("Missing required ConversationId");

        final String pa = (pbi != null ? pbi.getAction() : null);
        switch (Utils.compareStrings(sci.getAction(), pa)) {
            case -2 :
                throw new MessageSubmitException("Different Action values (submitted: " + sci.getAction()
                                                    + ",P-Mode: " + pa + ") specified!");
            case -1 :
                throw new MessageSubmitException("Missing required Action information");
            case 2 :
                sci.setAction(pa);
        }
        // This set will copy information into merged info set, therefor set after info is made complete
        cd.setCollaborationInfo(sci);

        completeService();
        completeAgreement();
    }

    /**
     * Completes the service information for the message. The service information is required for sending the message
     * and is used to fill the <code>eb:areEqual</code> element in the message header.
     * <p>Although service information is a composed information item, it must be provided completely by either
     * submitted meta-data or P-Mode.
     *
     * @throws MessageSubmitException   When service information is missing in both submitted meta-data and P-Mode or
     *                                  when the service information in submitted meta-data and P-Mode conflicts
     */
    private void completeService() throws MessageSubmitException {
        final IBusinessInfo pbi = (leg.getUserMessageFlow() != null ? leg.getUserMessageFlow().getBusinessInfo() : null );
        IService      psi = null;
        if (pbi != null)
            psi = pbi.getService();
        final Service ssi = (Service) cd.getCollaborationInfo().getService();

        if (ssi == null && psi == null)
            throw new MessageSubmitException("Missing required information on the receiver of the message");
        else if (ssi == null)
            // Take P-Mode info
            ((CollaborationInfo) cd.getCollaborationInfo()).setService(psi);
        else if (psi != null) {
            // Both P-Mode and submitted MMD contain service info, ensure they are equal
            if (!CompareUtils.areEqual(ssi, psi))
                throw new MessageSubmitException("Different values given for Service information!");
        }
    }

    /**
     * Completes the information on the referenced agreement. This is used for fill the <code>eb:AgreementRef</code> in
     * the header. If information on the agreement is given it must at least include the name and may include an
     * indication of the agreement type. Also the P-Mode id will be included.
     *
     * @throws MessageSubmitException   When agreement information included in either submitted meta-data or P-Mode does
     *                                  not include a name or when the service information in submitted meta-data and
     *                                  P-Mode conflicts
     */
    private void completeAgreement() throws MessageSubmitException {
        AgreementReference sar = (AgreementReference) cd.getCollaborationInfo().getAgreement();
        final IAgreement pa = pmode.getAgreement();

        if (sar == null && pa == null)
            return; // No agreement info in either submitted mmd or P-Mode
        else if (sar == null)
            sar = new AgreementReference();

        // Set the P-Mode id based on configuration
        final Boolean includeId = pmode.includeId();
        if (includeId != null && includeId)
            sar.setPModeId(pmode.getId());
        else
            sar.setPModeId(null);

        // Check name and type
        final String pan = (pa != null ? pa.getName() : null);
        switch (Utils.compareStrings(sar.getName(), pan)) {
            case -2 :
                throw new MessageSubmitException("Different Agreement name values (submitted: " + sar.getName()
                                                    + ",P-Mode: " + pa.getName() + ") specified!");
            case -1 :
                if (includeId)
                    // Only when the P-Mode id is included there must be an agreement reference
                    throw new MessageSubmitException("Missing required Agreement name information");
                else {
                    sar = null;
                    break;
                }
            case 2 :
                sar.setName(pan);
            case 1 :
                // Type should only be evaluated when a name is set (from P-Mode [case 2] or mmd [case 1])
                final String pat = (pa != null ? pa.getType() : null);
                switch (Utils.compareStrings(sar.getType(), pat)) {
                    case -2 :
                        throw new MessageSubmitException("Different Agreement type values (submitted: " + sar.getType()
                                                            + ",P-Mode: " + pa.getType()+ ") specified!");
                    case 2 :
                        sar.setType(pat);
                }
        }

        // Set the complete information in the merged set
        ((CollaborationInfo) cd.getCollaborationInfo()).setAgreement(sar);
    }

    /**
     * Merges the message properties specified in submitted meta-data and P-Mode. These properties are contained in
     * the <code>eb:MessageProperties</code> element in the header.
     * <br>When both sets contain the same property (name and type equal) the value from the submission is used.
     */
    private void completeProperties() {
        // The set of properties to use is the union of the properties from submission and P-Mode with properties from
        // submission taking precedence over the ones from the P-Mode
        final IBusinessInfo pbi = (leg.getUserMessageFlow() != null ? leg.getUserMessageFlow().getBusinessInfo() : null);
        if (pbi == null || pbi.getProperties() == null)
            return; // no properties from P-Mode

        // Get the collection of message properties provided with submission
        final Collection<IProperty> smp = cd.getMessageProperties();

        if (smp != null && !smp.isEmpty()) {
            // Check if the same property is also defined in both P-Mode and submitted data
            for (final IProperty p : pbi.getProperties()) {
                boolean found = false;
                for(final Iterator<IProperty> x = smp.iterator() ; !found && x.hasNext() ;) {
                    final IProperty xi = x.next();
                    found = xi.getName().equals(p.getName())
                        && (xi.getType() != null ? xi.getType().equals(p.getType()) : p.getType() == null);
                }
                if (!found) // Add the property from P-Mode when not already defined when submitted
                    cd.getMessageProperties().add(p);
            }
        } else {
            // No properties provided with submission, so use all properties provided in P-Mode
            cd.setMessageProperties(pbi.getProperties());
        }
    }


    /**
     * As instances of this class are only to be used internally the constructor is private. We use an internal instance
     * to simplify access to the supplied data which is now stored in the instance and not passed through in each method
     * call.
     */
    private MMDCompleter(final IUserMessage submittedMMD, final IPMode pmode) {
        this.pmode = pmode;

        // As we only support One-Way MEPs there is always just one leg
        this.leg = pmode.getLegs().iterator().next();

        // Start with a copy of the supplied MMD
        this.cd = new MessageMetaData(submittedMMD);
    }
}

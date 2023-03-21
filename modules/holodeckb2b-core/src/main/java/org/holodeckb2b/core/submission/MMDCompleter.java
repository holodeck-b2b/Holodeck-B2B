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
package org.holodeckb2b.core.submission;

import java.util.Collection;
import java.util.Iterator;

import org.holodeckb2b.common.messagemodel.AgreementReference;
import org.holodeckb2b.common.messagemodel.CollaborationInfo;
import org.holodeckb2b.common.messagemodel.Payload;
import org.holodeckb2b.common.messagemodel.Service;
import org.holodeckb2b.common.messagemodel.UserMessage;
import org.holodeckb2b.common.util.CompareUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.core.pmode.PModeUtils;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.messagemodel.Direction;
import org.holodeckb2b.interfaces.messagemodel.IPayload.Containment;
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
 * parameters from the P-Mode. Therefor this class uses an instance of {@link UserMessage} to create the complete
 * set of meta-data.
 * <p>This class is to be used only by the internal message submitter, therefore its visibility is limited to package.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
final class MMDCompleter {

    /**
     * The P-Mode that must be used to complete the MMD
     */
    private final IPMode        pmode;
    /**
     * The P-Mode parameters for the Leg the message is exchanged on
     */
    private final ILeg          leg;
    /**
     * The unified set of message meta data
     */
    private final UserMessage   submission;

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
    public static UserMessage complete(final IUserMessage submittedMMD, final IPMode pmode) throws MessageSubmitException {
        final MMDCompleter completer = new MMDCompleter(submittedMMD, pmode);

        return completer.complete();
    }

    /**
     * Completes the message meta data by combining the information from the submission and the P-Mode. When both
     * the submission and P-Mode contain the same parameters it is checked that these do not conflict (for most
     * parameters this means that they must be equal).
     *
     * @return  A complete set of message meta-data that can be processed bythe Core
     * @throws  MessageSubmitException  When the unified set does not contain all required meta-data required for
     *                                  sending the message, or<br>
     *                                  when a parameter exists in both sets but with a different value.
     */
    private UserMessage complete() throws MessageSubmitException {

        final String pmMPC = leg.getUserMessageFlow() != null && leg.getUserMessageFlow().getBusinessInfo() != null ?
                                leg.getUserMessageFlow().getBusinessInfo().getMpc() : null;
        switch (Utils.compareStrings(submission.getMPC(), pmMPC)) {
            case -2 :
                throw new MessageSubmitException("Different MPC values (submitted: " + submission.getMPC()
                                                + ",P-Mode: " + pmMPC + ") specified!");
            case -1 :
            	submission.setMPC(EbMSConstants.DEFAULT_MPC);
            case 2 :
                submission.setMPC(pmMPC);
        }

        completeSender();
        completeReceiver();
        completeCollaborationInfo();
        completeProperties();
        // Set the containment of payloads if not already specified
        if (!Utils.isNullOrEmpty(submission.getPayloads()))
        	submission.getPayloads().parallelStream().filter(p -> p.getContainment() == null)
        										 	 .forEach(p -> ((Payload) p).setContainment(Containment.ATTACHMENT));

        return submission;
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
        final ITradingPartner submissionSender = submission.getSender();
        Collection<IPartyId> sPartyIds = null;
        String  sRole = null;
        if (submissionSender != null) {
            sPartyIds = submissionSender.getPartyIds();
            sRole = submissionSender.getRole();
        }

        // Get sender info from P-Mode
        ITradingPartner pmodeSender = null;
        pmodeSender = PModeUtils.isHolodeckB2BInitiator(pmode) ? pmode.getInitiator() : pmode.getResponder();
        Collection<IPartyId> pmPartyIds = null;
        String  pmRole = null;
        if (pmodeSender != null) {
            pmPartyIds = pmodeSender.getPartyIds();
            pmRole = pmodeSender.getRole();
        }

        // Check PartyId(s)
        if (Utils.isNullOrEmpty(sPartyIds) && Utils.isNullOrEmpty(pmPartyIds))
            throw new MessageSubmitException("Missing PartyId information for Sender of the message!");
        else if (Utils.isNullOrEmpty(sPartyIds))
            // PartyId(s) specified in P-Mode
            submission.setSender(pmodeSender);
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
            submission.getSender().setRole(pmRole);
        else if (!Utils.isNullOrEmpty(pmRole)) {
            // Both submission and P-Mode specify Role => must be equal
            if (!pmRole.equals(sRole))
                throw new MessageSubmitException("Role of Sender in submission differs from one in selected P-Mode!");
        } else
            // Only submission contains Role, but since sender info may be overriden because P-Mode specified the
            // PartyId(s), set it again
            submission.getSender().setRole(sRole);
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
        final ITradingPartner mr = submission.getReceiver();
        Collection<IPartyId> sPartyIds = null;
        String  sRole = null;
        if (mr != null) {
            sPartyIds = mr.getPartyIds();
            sRole = mr.getRole();
        }
        // Get receiver info from P-Mode
        ITradingPartner pr = null;
        pr = PModeUtils.isHolodeckB2BInitiator(pmode) ? pmode.getResponder() : pmode.getInitiator();
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
            submission.setReceiver(pr);
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
            submission.getReceiver().setRole(pmRole);
        else if (!Utils.isNullOrEmpty(pmRole)) {
            // Both submission and P-Mode specify Role => must be equal
            if (!pmRole.equals(sRole))
                throw new MessageSubmitException("Role for Receiver in submission differs from one in selected P-Mode!");
        } else
            // Only submission contains Role, but since sender info may be overriden because P-Mode specified the
            // PartyId(s), set it again
            submission.getReceiver().setRole(sRole);
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
    	if (this.submission.getCollaborationInfo() == null)
        	this.submission.setCollaborationInfo(new CollaborationInfo());
        final CollaborationInfo sci = submission.getCollaborationInfo();
        final IBusinessInfo pbi = (leg.getUserMessageFlow() != null ? leg.getUserMessageFlow().getBusinessInfo() : null);

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

        completeService();
        completeAgreement();

        // Check that correct Service is specified when test Action URI is used
        if (submission.getCollaborationInfo().getAction().equals(EbMSConstants.TEST_ACTION_URI)
            && !submission.getCollaborationInfo().getService().getName().equals(EbMSConstants.TEST_SERVICE_URI))
            throw new MessageSubmitException("Service must be " + EbMSConstants.TEST_SERVICE_URI
                                             + "if Action is set to " + EbMSConstants.TEST_ACTION_URI);
    }

    /**
     * Completes the service information for the message. The service information is required for sending the message
     * and is used to fill the <code>eb:areEqual</code> element in the message header.
     * <p>The P-Mode may specify only the Service.type, in which it is combined with the Service.name value contained in
     * the submission. If both provide a value for type the values must match. Also if P-Mode contains only the
     * Service.name the submission must also contain only that same Service.name.
     *
     * @throws MessageSubmitException   When service information is missing in both submitted meta-data and P-Mode or
     *                                  when the service information in submitted meta-data and P-Mode conflicts
     */
    private void completeService() throws MessageSubmitException {
        final IBusinessInfo pbi = (leg.getUserMessageFlow() != null ? leg.getUserMessageFlow().getBusinessInfo() : null );
        IService      psi = null;
        if (pbi != null)
            psi = pbi.getService();
        final Service ssi = submission.getCollaborationInfo().getService();

        if (ssi == null && psi != null && !Utils.isNullOrEmpty(psi.getName()))
        	// Take P-Mode info
        	submission.getCollaborationInfo().setService(psi);
        else if (ssi != null && psi != null) {
        	switch (Utils.compareStrings(ssi.getName(), psi.getName())) {
        	case -2 :
        		throw new MessageSubmitException("Different values given for Service.name information!");
        	case -1 :
        		throw new MessageSubmitException("Missing required Service.name information of the message");
        	case 2 : // Use name from P-Mode
        		ssi.setName(psi.getName());
        	case 0 : // No name in P-Mode or equal, nothing to do
        	case 1 :
        	}
        	// Check if they both contain the type attribute
        	switch (Utils.compareStrings(ssi.getType(), psi.getType())) {
        	case -2 :
        		throw new MessageSubmitException("Different values given for Service.type information!");
        	case 1 : // Only submission contained type. Only allowed if the P-Mode didn't defined a Service name either
        		if (!Utils.isNullOrEmpty(psi.getName()))
        			throw new MessageSubmitException("Different values given for Service.type information!");
        		else
        			break;
        	case 2 :
        		ssi.setType(psi.getType());
        	case -1 : //  Both the submission and P-Mode didn't specify or used equal values
        	case 0 :
        	}
        } else if (ssi != null && Utils.isNullOrEmpty(ssi.getName()))
        	// Neither submission nor P-Mode contain at least the Service name
        	throw new MessageSubmitException("Missing required Service information of the message");
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
        AgreementReference sar = submission.getCollaborationInfo().getAgreement();
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
                if (includeId != null && includeId)
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
        submission.getCollaborationInfo().setAgreement(sar);
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
        final Collection<IProperty> smp = submission.getMessageProperties();

        if (!Utils.isNullOrEmpty(smp)) {
            // Check if the same property is also defined in both P-Mode and submitted data
            for (final IProperty p : pbi.getProperties()) {
                boolean found = false;
                for(final Iterator<IProperty> x = smp.iterator() ; !found && x.hasNext() ;) {
                    final IProperty xi = x.next();
                    found = xi.getName().equals(p.getName())
                        && (xi.getType() != null ? xi.getType().equals(p.getType()) : p.getType() == null);
                }
                if (!found) // Add the property from P-Mode when not already defined when submitted
                    submission.addMessageProperty(p);
            }
        } else {
            // No properties provided with submission, so use all properties provided in P-Mode
            submission.setMessageProperties(pbi.getProperties());
        }
    }


    /**
     * As instances of this class are only to be used internally the constructor is private. We use an internal instance
     * to simplify access to the supplied data which is now stored in the instance and not passed through in each method
     * call.
     */
    private MMDCompleter(final IUserMessage submittedMMD, final IPMode pmode) {
        this.pmode = pmode;
        this.leg = PModeUtils.getSendLeg(pmode);

        // Start with a copy of the supplied MMD
        this.submission = new UserMessage(submittedMMD);
        this.submission.setDirection(Direction.OUT);
    }
}

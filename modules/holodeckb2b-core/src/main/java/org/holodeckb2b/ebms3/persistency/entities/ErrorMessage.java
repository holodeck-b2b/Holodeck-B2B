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
package org.holodeckb2b.ebms3.persistency.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Is the JPA entity class representing an ebMS <b>Error Signal</b> message unit processed by Holodeck B2B.
 * <p>Extends {@link MessageUnit} and adds the meta-data specific to the Error Signal which is the list of errors
 * contained in the signal and an indication whether a SOAP Fault should be added when sending the message. Also this
 * class overrides the {@link #getRefToMessageId()} because the message unit in error can be referenced both by the
 * <i>refToMessageId</i> from the Error Signal itself or by the <i>refToMessageInError</i> from one of the Errors
 * contained in the signal.<br>
 * Furthermore two queries are defined:<ul>
 * <li><i>ErrorMessage.findForPModesInState</i> finds all Error Signals that are in a certain processing state and that
 *              are processed by one of the given P-Modes</li>
 * <li><i>ErrorMessage.findResponsesTo</i> finds all Error Signals that are a response to another message unit, i.e.
 *              which refer to the given message id.</li></ul>
*
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name="ERROR_MESSAGE")
@DiscriminatorValue("ERRORMSG")
@NamedQueries({
        @NamedQuery(name="ErrorMessage.findResponsesTo",
            query = "SELECT err " +
                    "FROM ErrorMessage err " +
                    "WHERE err.REF_TO_MSG_ID = :refToMsgId " +
                    "OR (err.REF_TO_MSG_ID IS NULL AND :refToMsgId IN (SELECT e.REF_TO_MSG_IN_ERROR FROM err.errors e))"
            )
        }
)
public class ErrorMessage extends SignalMessage implements IErrorMessage {

    @Override
    public Collection<IEbmsError> getErrors() {
        return errors;
    }

    public void setErrors(final Collection<EbmsError> errorList) {
        if (errors == null)
            errors = new ArrayList<>();
        errors.clear();
        errors.addAll(errorList);
    }

    public void addError(final EbmsError e) {
        if (errors == null)
            errors = new ArrayList<>();
        errors.add(e);
    }

    /**
     * Gets indicator whether this Error Signal should be combined with a SOAP Fault.
     * <p>NOTE: The SOAP Fault will only be added if the Error signal is not bundled with other message units.
     *
     * @return  <code>true</code> if Error Signal should be combined with SOAP Fault,<br>
     *          <code>false</code> when not
     */
    public boolean shouldHaveSOAPFault() {
        return ADD_SOAP_FAULT;
    }

    /**
     * Sets the indicator whether this Error Signal should be combined with a SOAP Fault.
     *
     * @param addFault boolean indicator whether SOAP Fault should be added to this Error signal
     */
    public void setAddSOAPFault(final boolean addFault) {
        this.ADD_SOAP_FAULT = addFault;
    }

    /**
     * Gets the message id of the referenced message unit. The referenced message id can be included in either the
     * <code>eb:/SignalMessage/eb:MessageInfo/eb:RefToMessageId</code> element in the ebMS header or the
     * <code>eb:/SignalMessage/eb:Error/@refToMessageInError</code> attribute in the signal message.
     * <p>The attribute can occur multiple times as there may be more than one Error included in the signal. The ebMS
     * specification requires that the signal can reference at most one message unit, so all refer the same messageId
     * or none at all.
     *
     * @return              The referenced message id in the Error signal if it is valid, or<br>
     *                      <code>null</code> when the error does not reference one message unit
     */
    @Override
    public String getRefToMessageId() {

        // First get RefToMessageId from header by calling super class
        String refToMessageId = super.getRefToMessageId();

        if (refToMessageId == null || refToMessageId.isEmpty()) {
            // Then check individual error, if the RefToMessageId element from header contained a id, all other ids
            // should be the same (or null)
            final Iterator<IEbmsError> it = this.getErrors().iterator();
            while (it.hasNext() && (refToMessageId == null || refToMessageId.isEmpty()))
                refToMessageId = it.next().getRefToMessageInError();
        }

        return refToMessageId;
    }

    /**
     * Returns a {@see String} representation of the error signal. This method can be
     * used for easily logging the error to text files, etc.
     *
     * @return {@see String} representation of the error signal
     */
    @Override
    public String toString() {
        final StringBuilder  errorMsg = new StringBuilder("ErrorSignal: msgId=");
        errorMsg.append(getMessageId()).append(", referenced message id=").append(getRefToMessageId()).append("\n");
        errorMsg.append("List of errors:");

        for(final IEbmsError error : getErrors())
            errorMsg.append(error.toString()).append("\n");

        return errorMsg.toString();
    }

    /*
     * Fields
     *
     * NOTES:
     * 1) The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */

    /**
     * The list of errors contained in this Error signal
     */
    @ElementCollection(targetClass = EbmsError.class)
    @CollectionTable(name="ERR_MU_ERRORS")
    private List<IEbmsError>       errors;

    /**
     * Indicator whether this Error signal should be combined with a SOAP Fault.
     * <p>NOTE: The SOAP Fault will only be added if the Error signal is not bundled with other message units.
     */
    private boolean     ADD_SOAP_FAULT = false;
}

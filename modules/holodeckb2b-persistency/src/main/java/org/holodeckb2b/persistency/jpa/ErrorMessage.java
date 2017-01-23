/**
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Is the JPA entity class for storing the meta-data of an ebMS <b>Error Signal</b> message unit as described by the
 * {@link IErrorMessage} interface in the Holodeck B2B messaging model.
 * <p>One query is defined:<ul>
 * <li><i>ErrorMessage.findResponsesTo</i> finds all Error Signals that are a response to another message unit, i.e.
 *              which refer to the given message id.</li></ul>
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Entity
@Table(name="ERROR_MESSAGE")
@DiscriminatorValue("ERRORMSG")
public class ErrorMessage extends MessageUnit implements IErrorMessage, Serializable {

    @Override
    public Collection<IEbmsError> getErrors() {
        return errors;
    }

    public void setErrors(Collection<IEbmsError> errors) {
        if (!Utils.isNullOrEmpty(errors)) {
            this.errors = new ArrayList<>();
            for (IEbmsError e : errors)
                this.errors.add(new EbmsError(e));
        } else
            this.errors = null;
    }

    public void addError(IEbmsError error) {
        if (error != null) {
            if (this.errors == null)
                this.errors = new ArrayList<>();
            this.errors.add(new EbmsError(error));
        }
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

    /*
     * Constructors
     */
    /**
     * Default constructor creates an empty <code>ErrorMessage</code> object
     */
    public ErrorMessage() {}

    /**
     * Create a new <code>ErrorMessage</code> object for the error signal message unit described by the given
     * {@link IErrorMessage} object.
     *
     * @param sourceUserMessage   The meta data of the user message unit to copy to the new object
     */
    public ErrorMessage(final IErrorMessage sourceUserMessage) {
        super(sourceUserMessage);

        if (sourceUserMessage == null)
            return;
        else
            setErrors(sourceUserMessage.getErrors());
    }

    /**
     * Create a new <code>ErrorMessage</code> object that contains the given error.
     *
     * @param error
     */
    public ErrorMessage(final IEbmsError error) {
        this();
        addError(error);
    }

    /**
     * Create a new <code>ErrorMessage</code> object that contains the given errors.
     *
     * @param errors
     */
    public ErrorMessage(final Collection<IEbmsError> errors) {
        this();
        setErrors(errors);
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

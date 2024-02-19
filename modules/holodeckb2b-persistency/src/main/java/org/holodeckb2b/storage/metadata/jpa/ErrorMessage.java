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
package org.holodeckb2b.storage.metadata.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.storage.IErrorMessageEntity;

/**
 * Is the JPA entity class for storing the meta-data of an ebMS <b>Error Signal</b> message unit as described by the
 * {@link IErrorMessageEntity} interface in the Holodeck B2B persistency model. The class however does not implement 
 * this interface as it is not the actual entity provided to the Core.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
@Entity
@Table(name="ERROR_MESSAGE")
@DiscriminatorValue("ERRORMSG")
public class ErrorMessage extends MessageUnit {
    private static final long serialVersionUID = 130225048931288817L;

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

    /**
     * Gets the label of the leg within the P-Mode on which this Error Message is exchanged. Although the Leg can in
     * most cases be calculated there can be an issue when there is no explicit reference to the message unit in error 
     * and there are more than one sent message units in the message the Error Message is a reply to. In that case the
     * P-Mode and Leg of the primary message unit from the sent message are used. But this information is not persisted
     * and therefore the leg is stored with the Error Message.    
     * 
     * @return  The leg label
     * @since 6.0.0
     */
    public ILeg.Label getLeg() {
    	return LEG;
    }
    
    /**
     * Sets the label of the leg within the P-Mode on which this Error Message is exchanged. 
     * 
     * @param label	of the Leg
     * @since 6.0.0
     */
    public void setLeg(ILeg.Label label) {
    	this.LEG = label;
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
    
    /**
     * The label of the Leg that governs the processing of the Error Message. 
     */
    @Enumerated(EnumType.STRING)
    private ILeg.Label	LEG;

}

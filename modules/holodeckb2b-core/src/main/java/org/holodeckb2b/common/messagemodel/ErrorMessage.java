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
package org.holodeckb2b.common.messagemodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.holodeckb2b.commons.util.MessageIdUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;

/**
 * Is an in memory only implementation of {@link IErrorMessage} to temporarily store the meta-data information on a
 * Error Signal message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class ErrorMessage extends MessageUnit implements IErrorMessage {
	private ArrayList<IEbmsError>    errors;

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
        setMessageId(MessageIdUtils.createMessageId());
        setTimestamp(new Date());
        addError(error);
    }

    /**
     * Create a new <code>ErrorMessage</code> object that contains the given errors.
     *
     * @param errors
     */
    public ErrorMessage(final Collection<IEbmsError> errors) {
        this();
        setMessageId(MessageIdUtils.createMessageId());
        setTimestamp(new Date());
        setErrors(errors);
    }

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
}

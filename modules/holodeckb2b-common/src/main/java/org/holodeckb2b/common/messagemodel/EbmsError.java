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

import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;

/**
 * Is an in memory only implementation of {@link IEbmsError} to temporarily store the meta-data on a ebMS Error that is
 * contained in an Error Signal message unit.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class EbmsError implements IEbmsError {

    private String      errorCode;
    private Severity    severity;
    private String      errorMessage;
    private String      origin;
    private String      category;
    private String      refToMsgInError;
    private String      errorDetail;
    private Description longDescription;

    /**
     * Default constructor creates an empty object
     */
    public EbmsError() {
    }

    /**
     * Creates a new <code>EbmsError</object> using the meta-data from the given source object.
     *
     * @param source    The source object to copy the data from
     */
    public EbmsError(final IEbmsError source) {
        this.errorCode = source.getErrorCode();
        this.severity = source.getSeverity();
        this.errorMessage = source.getMessage();
        this.origin = source.getOrigin();
        this.category = source.getCategory();
        this.refToMsgInError = source.getRefToMessageInError();
        this.errorDetail = source.getErrorDetail();
        setDescription(source.getDescription());
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(final Severity severity) {
        this.severity = severity;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }

    public void setMessage(final String message) {
        this.errorMessage = message;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(final String origin) {
        this.origin = origin;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    @Override
    public String getRefToMessageInError() {
        return refToMsgInError;
    }

    public void setRefToMessageInError(final String refToMsgInError) {
        this.refToMsgInError = refToMsgInError;
    }

    @Override
    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(final String errorDetail) {
        this.errorDetail = errorDetail;
    }

    @Override
    public IDescription getDescription() {
        return longDescription;
    }

    public void setDescription(final IDescription description) {
        this.longDescription = description != null ? new Description(description) : null;
    }
}

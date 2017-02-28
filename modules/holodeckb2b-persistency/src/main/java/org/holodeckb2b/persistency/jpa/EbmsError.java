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
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;

/**
 * Is an <i>embeddable</i> JPA persistency class used to store the meta-data of one ebMS Error ad described by {@link
 * IEbmsError} interface in the Holodeck B2B messaging model. The text in the error message and detail field that
 * provide more information on the error is limited to 1024 respectively 10K characters.
 * <p>As an individual ebMS Error is always part of an Error Signal message unit this class is defined as <i>
 * embeddable</i>.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION
 */
@Embeddable
public class EbmsError implements IEbmsError, Serializable {

    /*
     * Getters and setters
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    public void setCategory(final String category) {
        CATEGORY = category;
    }

    @Override
    public String getRefToMessageInError() {
        return REF_TO_MSG_IN_ERROR;
    }

    public void setRefToMessageInError(final String refToMsg) {
        REF_TO_MSG_IN_ERROR = refToMsg;
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    public void setErrorCode(final String errorCode) {
        ERROR_CODE = errorCode;
    }

    @Override
    public Severity getSeverity() {
        return SEVERITY;
    }

    public void setSeverity(final IEbmsError.Severity severity) {
        SEVERITY = severity;
    }

    @Override
    public String getMessage() {
        return ERROR_MESSAGE;
    }

    public void setShortDescription(final String message) {
        ERROR_MESSAGE = message;
    }

    @Override
    public String getErrorDetail() {
        return ERROR_DETAIL;
    }

    public void setErrorDetail(final String errorDetail) {
        ERROR_DETAIL = errorDetail;
    }

    @Override
    public String getOrigin() {
        return ORIGIN;
    }

    public void setOrigin(final String origin) {
        ORIGIN = origin;
    }

    @Override
    public IDescription getDescription() {
        return longDescription;
    }

    public void setDescription(final IDescription description) {
        longDescription = description != null ? new Description(description) : null;
    }

    /*
    * Constructors
    */
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
        this.ERROR_CODE = source.getErrorCode();
        this.SEVERITY = source.getSeverity();
        this.ERROR_MESSAGE = source.getMessage();
        this.ORIGIN = source.getOrigin();
        this.CATEGORY = source.getCategory();
        this.REF_TO_MSG_IN_ERROR = source.getRefToMessageInError();
        this.ERROR_DETAIL = source.getErrorDetail();
        if (source.getDescription() != null)
            setDescription(new Description(source.getDescription()));
    }

    /*
     * Fields
     *
     * NOTE: The JPA @Column annotation is not used so the attribute names are
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    private String          CATEGORY;

    private String          ERROR_CODE;

    @Lob
    @Column(length = 10000)
    private String          ERROR_DETAIL;

    private String          ORIGIN;

    private String          REF_TO_MSG_IN_ERROR;

    @Lob
    @Column(length = 1024)
    private String          ERROR_MESSAGE;

    @Enumerated(EnumType.STRING)
    private IEbmsError.Severity SEVERITY;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "language", column = @Column(name = "DESCRIPTION_LANG")),
        @AttributeOverride(name = "text", column = @Column(name = "DESCRIPTION_TXT", length = 10000))
    })
    private Description     longDescription;
}

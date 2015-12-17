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
package org.holodeckb2b.ebms3.persistent.message;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import org.holodeckb2b.ebms3.persistent.general.Description;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError.Severity;

/**
 * Is the persistency class that represents one error contained in an ebMS <i>Error
 * signal message unit</i>. Contains the details that specify what error occurred.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Embeddable
public class EbmsError implements Serializable, IEbmsError {

    /**
     * Default constructor, creates an empty ebMS error.
     */
    public EbmsError() {}
    
    /**
     * Constructs a new ebMS error with the given values for the specific fields.
     * 
     * @param errorCode     
     * @param severity
     * @param refToMsgId
     * @param message
     * @param detail
     * @param origin
     * @param category
     * @param description 
     */
    public EbmsError(final String errorCode, final Severity severity, final String refToMsgId, final String shortDescr, 
                     final String detail, final String origin, final String category, final Description description) {
        this.ERROR_CODE = errorCode;
        this.SEVERITY = severity;
        this.REF_TO_MSG_IN_ERROR = refToMsgId;
        this.SHORT_DESCR = shortDescr;
        this.ERROR_DETAIL = detail;
        this.ORIGIN = origin;
        this.CATEGORY = category;
        this.longDescription = description;        
    }
    
    /*
     * Getters and setters
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }
    
    public void setCategory(String category) {
        CATEGORY = category;
    }

    @Override
    public String getRefToMessageInError() {
        return REF_TO_MSG_IN_ERROR;
    }

    public void setRefToMessageInError(String refToMsg) {
        REF_TO_MSG_IN_ERROR = refToMsg;
    }
    
    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
    
    public void setErrorCode(String errorCode) {
        ERROR_CODE = errorCode;
    }

    @Override
    public Severity getSeverity() {
        return SEVERITY;
    }
    
    public void setSeverity(IEbmsError.Severity severity) {
        SEVERITY = severity;
    }

    @Override
    public String getMessage() {
        return SHORT_DESCR;
    }
    
    public void setShortDescription(String message) {
        SHORT_DESCR = message;
    }

    @Override
    public String getErrorDetail() {
        return ERROR_DETAIL;
    }

    public void setErrorDetail(String errorDetail) {
        ERROR_DETAIL = errorDetail;
    }

    @Override
    public String getOrigin() {
        return ORIGIN;
    }
    
    public void setOrigin(String origin) {
        ORIGIN = origin;
    }

    @Override
    public IDescription getDescription() {
        return longDescription;
    }
    
    public void setDescription(Description description) {
        longDescription = description;
    }
    
    /**
     * Returns a {@see String} representation of the error. This method can be
     * used for easily logging the error to text files, etc.
     * 
     * @return {@see String} representation of the error
     */
    @Override
    public String toString() {
        return "ebMS error: " + (REF_TO_MSG_IN_ERROR != null ? "msgId of msg in error= " + REF_TO_MSG_IN_ERROR : "")
                + "\n\terror code= " + ERROR_CODE + ", " 
                + "\n\tshort description= " + SHORT_DESCR + ", "
                + "\n\tseverity= " + SEVERITY.toString()
                + (ORIGIN != null ? "\n\t, origin= " + ORIGIN : "") 
                + (CATEGORY != null ? "\n\t, category= " + CATEGORY : "") 
                + (ERROR_DETAIL != null ? "\n\t, details= " + ERROR_DETAIL : "") 
                + (longDescription != null ? "\n\t, long description= " +
                        (longDescription.getLanguage() != null ? 
                                    "[" + longDescription.getLanguage() + "]" : "") 
                        + longDescription.getText()
                    : "");
    }
    
    /*
     * Fields
     * 
     * NOTE: The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     */
    private String          CATEGORY;
    
    private String          ERROR_CODE;
    
    @Column(name = "ERROR_DETAIL", length = 10000)
    private String          ERROR_DETAIL;

    private String          ORIGIN;
    
    private String          REF_TO_MSG_IN_ERROR;
    
    private String          SHORT_DESCR;
   
    @Enumerated(EnumType.STRING)
    private IEbmsError.Severity SEVERITY;
        
    @Embedded
    private Description     longDescription;
}

/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.mmd.xml;

import java.util.ArrayList;
import java.util.Collection;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 * Represents the <code>PayloadInfo</code> element from the MMD document. This element contains the meta-data on the
 * paylaod data (to be) included in the User Message. The <code>PartInfo</code> child elements contain the information 
 * about the specific payload. This element has just one attribute <code>deleteFilesAfterSubmit</code> which indicates
 * whether the files containing the payload data should be deleted after successful submission to the Holodeck B2B Core.
 * <p>NOTE: This class only creates the possibility to exchange the <i>"delete flag"</i>, it is up to the actual submit
 * implementation to use it, i.e. really ensure files are deleted.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
class PayloadInfo {
    
    /**
     * The <code>deleteFilesAfterSubmit</code> attribute, default value is false
     */
    @Attribute(name = "deleteFilesAfterSubmit", required = false)
    private Boolean deleteFilesAfterSubmit;
    
    /**
     * The list of <code>PartInfo</code> child elements, must be at least one
     */
    @ElementList(entry = "PartInfo", inline = true, type = PartInfo.class, required=true)
    private ArrayList<IPayload>  payloads;
    
    /**
     * Default constructor creates empty meta data object.
     */
    PayloadInfo() {       
    }
    
    /**
     * Creates a new PayloadInfo with the provided meta-data about the payloads. The "delete flag" is not set.
     * 
     * @param payloadInfo The meta-data on the individual payloads
     */
    PayloadInfo(Collection<IPayload> payloadInfo) {
        this(payloadInfo, null);
    }
    
    /**
     * Creates a new PayloadInfo with the provided meta-data about the payloads and sets "delete flag" to given value.
     * 
     * @param payloadInfo           The meta-data on the individual payloads
     * @param deleteAfterSubmit     Value to use for the "delete flag"
     */
    PayloadInfo(Collection<IPayload> payloadInfo, Boolean deleteAfterSubmit) {
        this.deleteFilesAfterSubmit = deleteAfterSubmit;
        setPayloadInfo(payloadInfo);
    }
    
    /**
     * Set the meta-data on the actual payloads.
     * 
     * @param payloadInfo The meta-data on the individual payloads
     */
    void setPayloadInfo(Collection<IPayload> payloadInfo) {
        if (!Utils.isNullOrEmpty(payloadInfo)) {
            this.payloads = new ArrayList<IPayload>(payloadInfo.size());
            for(IPayload p : payloadInfo)
                this.payloads.add(new PartInfo(p));
        } else
            this.payloads = null;
    }

    /**
     * Adds meta-data about one payload to the existing set of payload meta-data. Will create list of payload data if
     * not already done.
     * 
     * @param p The meta-data on the specific payload
     */
    void addPayload(IPayload p) {
        if (payloads == null) 
            payloads = new ArrayList<IPayload>(1);
        
        payloads.add(new PartInfo(p));
    }
    
    /** 
     * Gets the meta-data about the individual payloads
     * 
     * @return The information will be returned as a Collection of IPayload implementations.
     */
    Collection<IPayload> getPayloads() {
        return payloads;
    }
    
    /**
     * Sets the indicator whether the payload files should be deleted after successful submission to the Holodeck B2B
     * Core.
     * 
     * @param delete The new value for the indicator
     */
    void setDeleteFilesAfterSubmit(boolean delete) {
        this.deleteFilesAfterSubmit = delete;
    }
    
    /**
     * Gets the indicator whether the payload files should be deleted after successful submission to the Holodeck B2B
     * Core.
     * 
     * @return <code>true</code> when the files should be deleted, <code>false</code> if not. 
     */
    boolean shouldDeleteFilesAfterSubmit() {
        return (this.deleteFilesAfterSubmit != null ? this.deleteFilesAfterSubmit.booleanValue() : false);
    }
} 

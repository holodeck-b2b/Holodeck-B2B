/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.holodeckb2b.common.exceptions.ObjectSerializationException;
import org.holodeckb2b.common.general.IAuthenticationInfo;
import org.holodeckb2b.common.messagemodel.IPullRequest;
import org.holodeckb2b.common.util.Utils;

/**
 * Is a persistency class representing an ebMS PullRequest messaage unit that 
 * is processed by Holodeck B2B. 
 * <p>NOTE: Storage of the authentication information is currently done by 
 * storing this information as a binary object by serializing the object. In
 * further version this should be refined!
 * @todo: Refine storage of the authentication info
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
@Entity
@Table(name="PULLREQUEST")
@DiscriminatorValue("PULLREQ")
public class PullRequest extends SignalMessage implements IPullRequest {

    /*
     * Getters and setters
     */
    @Override
    public String getMPC() {
        return MPC;
    }

    public void setMPC(String newMPC) {
        this.MPC = newMPC;
    }
    
    @Override
    public IAuthenticationInfo getAuthenticationInfo() {
        if (authInfo == null){
            try {
                authInfo = (IAuthenticationInfo) Utils.deserialize(getAIObject());
            } catch (ObjectSerializationException ex) {
                // Deserialization failed, no object available!
                authInfo = null;
            }
        }
        
        return authInfo;
    }
    
    public void setAuthenticationInfo(IAuthenticationInfo ai) {
        // Beside setting the normal object also store it serialized
        authInfo = ai;

        try {
            setAIObject(Utils.serialize(ai));
        } catch (ObjectSerializationException ex) {
            // Something went wrong serializing the object. To ensure integrity
            // also reset "normal" version
            authInfo = null;
        }
    }
    
    /*
     * Because authentication info is stored by serializing the <code>IAuthenticationInfo</code>
     * we need additional getter and setter methods to store and read the bytes from the
     * database
     */
    private byte[] getAIObject() {
        return AUTH_INFO_OBJ;
    }
    
    private void setAIObject(byte[] serializedAI) {
        AUTH_INFO_OBJ = serializedAI;
    }
    
    /*
     * Fields
     * 
     * NOTES: 
     * 1) The JPA @Column annotation is not used so the attribute names are 
     * used as column names. Therefor the attribute names are in CAPITAL.
     * 2) The primary key field is inherited from super class
     */
    
    private String          MPC;
    
    /*
     * The authentication info is saved by serializing the <code>IAuthenticationInfo</code>
     * object, therefor this column is a binary object, for easy access also 
     * a <i>transient</i> object is defined to store the object temporarely as
     * an object
     */
    @Lob
    @Column(name = "AUTH_INFO_OBJ", length = 1999999999)
    private byte[]          AUTH_INFO_OBJ;
    
    @Transient
    private IAuthenticationInfo     authInfo;
}


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
package org.holodeckb2b.security.callbackhandlers;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.wss4j.common.ext.WSPasswordCallback;

/**
 * Is used to hand over passwords to the WSS4J library. The callback handler does not retrieve the passwords from the
 * configuration itself. Username password combinations should be set before WSS4J starts processing the message. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PasswordCallbackHandler implements CallbackHandler {
    
    /**
     * The list of username, password combinations
     */
    private Map<String, String>  userPwds = new HashMap<String, String>();

    /**
     * Add a username, password combination to the callback handler.
     * 
     * @param username  
     * @param password 
     */
    public void addUser(String username, String password) {
        userPwds.put(username, password);
    }
    
    /**
     * Handles the callback for the password from the WSS4J library. It will set the password to <code>null</code> if 
     * there is no password registered for the identifier (i.e. username) specified by the callback.
     * 
     * @param callbacks     An array of callbacks that should handled. This handler will only process the first callback
     * @throws UnsupportedCallbackException     When this handler is called incorrecty
     */    
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
 
        if (!(callbacks[0] instanceof WSPasswordCallback))
            throw new UnsupportedCallbackException(callbacks[0], "Can not handle this type of callback!");
        
        WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
        pc.setPassword(userPwds.get(pc.getIdentifier()));        
    }
}

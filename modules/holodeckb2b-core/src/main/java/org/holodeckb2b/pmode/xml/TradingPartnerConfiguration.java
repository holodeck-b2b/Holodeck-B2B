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
package org.holodeckb2b.pmode.xml;


import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.holodeckb2b.interfaces.pmode.ISecurityConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;

/**
 * Contains the P-Mode parameters for the trading partners involved in the message exchange. In the P-Mode XML document
 * the trading partners occur in the element <code>Initiator</code> and <code>Responder</code> to reflect their role
 * in the MEP.
 * <p>Although the ebMS specification suggests that the security settings can be specified on the message unit level
 * it is assumed in Holodeck B2B that the security is specified on the trading partner level and that all messages from
 * one trading partner within an exchange use the same security settings.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Root
public class TradingPartnerConfiguration implements ITradingPartnerConfiguration {

    /*
     * The list of PartyIds that identify this trading partner. When used all PartyIds from the child elements will be
     * included the User Message. If the submitter should be able to set the PartyId, don't include this element
     */
    @ElementList(entry = "PartyId", type = PartyId.class , required = false, inline = true)
    private ArrayList<IPartyId> partyIds;

    /*
     * Optionally a <i>role</i> might be provided
     */
    @Element(name="Role", required = false)
    private String role;

    /**
     * The security settings used by the trading partner
     */
    @Element(name="SecurityConfiguration", required = false)
    private SecurityConfiguration   securityConfig;

    /**
     * Checks that the trading partner configuration included in the P-Mode XML document includes at least a PartyId or
     * security configuration.
     *
     * @throws PersistenceException     When neither PartyId or security configuration is included in the XML document
     */
    @Validate
    public void validate() throws PersistenceException {
        if (Utils.isNullOrEmpty(partyIds) && securityConfig == null)
            throw new PersistenceException("Either one or more PartyIds or the security configuration must be included");
    }

    /**
     * Get the TradingPartner party id's.
     *
     * @return  Collection of Part id's.
     */
    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    /**
     * Get the TradingPartner role.
     * @return String The role of the TradingPartner.
     */
    @Override
    public String getRole() {
        return role;
    }

    @Override
    public ISecurityConfiguration getSecurityConfiguration() {
        return securityConfig;
    }


}
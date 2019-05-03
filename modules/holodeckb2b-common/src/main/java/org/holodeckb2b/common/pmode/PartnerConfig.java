/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters related to the trading partners.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class PartnerConfig implements ITradingPartnerConfiguration, Serializable {
	private static final long serialVersionUID = -5890686664768972324L;

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
    private SecurityConfig   securityConfig;
    
    /**
     * Default constructor creates a new and empty <code>PartnerConfig</code> instance.
     */
    public PartnerConfig() {}

    /**
     * Creates a new <code>PartnerConfig</code> instance using the parameters from the provided {@link
     * ITradingPartnerConfiguration}  object.
     *
     * @param source The source object to copy the parameters from
     */
    public PartnerConfig(final ITradingPartnerConfiguration source) {
        this.role = source.getRole();
        this.securityConfig = source.getSecurityConfiguration() != null ? 
        												new SecurityConfig(source.getSecurityConfiguration()) : null;
        Collection<IPartyId> sourcePartyIds = source.getPartyIds();
        if (!Utils.isNullOrEmpty(sourcePartyIds)) {
            this.partyIds = new ArrayList<>(sourcePartyIds.size());
            sourcePartyIds.forEach((pid) -> this.partyIds.add(new PartyId(pid)));
        }
    }

    @Override
    public SecurityConfig getSecurityConfiguration() {
        return securityConfig;
    }

    public void setSecurityConfiguration(final SecurityConfig secConfig) {
        this.securityConfig = secConfig;
    }

    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    public void addPartyId(final PartyId partyId) {
        if (this.partyIds == null)
            this.partyIds = new ArrayList<>();

        this.partyIds.add(partyId);
    }

    public void setPartyIds(final Collection<PartyId> partyIds) {
        if (partyIds != null) {
            this.partyIds = new ArrayList<>(partyIds.size());
            partyIds.forEach(pid -> this.partyIds.add(pid));
        } else
            this.partyIds = null;
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }
}

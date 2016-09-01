/*
 * Copyright (C) 2015 The Holodeck B2B Team
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
package org.holodeckb2b.pmode.helpers;

import java.util.Collection;
import java.util.HashSet;
import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.pmode.ITradingPartnerConfiguration;

/**
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class PartnerConfig implements ITradingPartnerConfiguration {

    private String                  role;
    private Collection<IPartyId>    partyIds;
    private SecurityConfig          secConfig;


    @Override
    public SecurityConfig getSecurityConfiguration() {
        return secConfig;
    }

    public void setSecurityConfiguration(final SecurityConfig secConfig) {
        this.secConfig = secConfig;
    }

    @Override
    public Collection<IPartyId> getPartyIds() {
        return partyIds;
    }

    public void addPartyId(final PartyId partyId) {
        if (this.partyIds == null)
            this.partyIds = new HashSet<>();

        if (partyId != null)
            this.partyIds.add(partyId);
    }

    @Override
    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }
}

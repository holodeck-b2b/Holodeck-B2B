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
package org.holodeckb2b.interfaces.pmode;



import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.holodeckb2b.interfaces.pmode.security.ISecurityConfiguration;

/**
 * Represents the configuration of a trading partner involved in the message exchange. Extends the general {@link 
 * ITradingPartner} interface with the security configuration that should be used when exchanging messages with this
 * trading partner.
 * <p>In the ebMS specification the security parameters are defined on the leg level (or maybe even finer grained per 
 * message, but that is not really clear). Within Holodeck B2B we assume however that security settings are determined 
 * by the trading partners involved in the message exchange. Therefor the security configuration is part of the trading
 * partner configuration in the P-Mode.
 * 
 * <p>The security configuration contains the settings that are specific to this trading partner; the settings related 
 * to signing define how this trading partner will sign it messages, and the encryption settings define how messages to
 * this trading partner must be encrypted.<br>
 * This means that the security configuration of trading partner for which Holodeck B2B is the MSH contains the 
 * <b>private keys</b> and that the security configuration for the other partner contains the <b>public keys</b>. 
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see ITradingPartner
 */
public interface ITradingPartnerConfiguration extends ITradingPartner {

    /**
     * Gets the security configuration settings for this trading partner. 
     * 
     * @return  A {@link ISecurityConfiguration} object containing the security settings for the trading partner, or<br>
     *          <code>null</code> when there are none.
     */
    public ISecurityConfiguration getSecurityConfiguration();
}

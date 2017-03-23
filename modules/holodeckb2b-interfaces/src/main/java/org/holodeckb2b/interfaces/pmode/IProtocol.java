/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.pmode;

/**
 * Defines the settings for the SOAP and transport protocol used for exchanging the messages.
 * <p>Consists of the URL where the other MSH can be found, the SOAP version to use and whether HTTP compression and/or
 * chunking must be used. If the exchange uses AS4 multi-hop the URL is of the intermediary and not the endpoint. The
 * message must include routing information to enable the intermediaries in the I-Cloud to find the destination (see
 * <a href="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/part2/201004/cs01/ebms-v3.0-part2-cs01.html#__RefHeading__34462446">
 * section 2 of the ebMS 3 Part 2 Adv. Features spec</a>). To indicate that the routing information must be added the
 * P-Mode parameter <b>AddActorOrRoleAttribute</b> is introduced. It indicates if the ebMS message header
 * (<code>eb:Messaging</code> element) must be targeted to a specific SOAP target role/actor.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface IProtocol {

    /**
     * Gets the URL where the message should be sent to.
     *
     * @return The destination address
     */
    public String getAddress();

    /**
     * Indicates whether the ebMS message header, i.e. the <code>eb:Messaging</code> element, must be targeted to the
     * <i>http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/nextmsh</i> SOAP target role/actor.
     *
     * @return  <code>true</code> if the SOAP target role/actor must be added to the ebMS message header,<br>
     *          <code>false</code> if the default target should be used.
     * @todo: Set version number
     * @since 
     */
    public boolean shouldAddActorOrRoleAttribute();

    /**
     * Gets the SOAP version to use for packaging the ebMS message.
     *
     * @return  "1.1" when SOAP 1.1 should be used, "1.2" for SOAP 1.2
     * @todo: Consider using enum?
     */
    public String getSOAPVersion();

    /**
     * Indicates whether the HTTP <i>"chunked"</i> transfer encoding should be used. See section 3.6 of the HTTP/1.1
     * protocol [RFC2616] for more details.
     * <p>Using the gzip content encoding requires the use of the chunked transfer encoding. Therefor Holodeck B2B will
     * first check whether compression content encoding must be used an ignore the "chunked" setting.
     *
     * @return  <code>true</code> when the <i>"chunked"</i> encoding should be used,<br>
     *          <code>false</code> otherwise
     * @see #useHTTPCompression()
     */
    public boolean useChunking();

    /**
     * Indicates whether the HTTP <i>"gzip"</i> compression content encoding should be used. See section 3.5 of the
     * HTTP/1.1 protocol [RFC2616] for more details.
     *
     * @return  <code>true</code> when the <i>"gzip"</i> content encoding should be used,<br>
     *          <code>false</code> otherwise
     */
    public boolean useHTTPCompression();

}

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
package org.holodeckb2b.multihop;

/**
 * Defines common literals in handling multi-hop messaging. See section 2 of the ebMS V3 Part 2 specification for more
 * information about the ebMS 3 multi-hop function.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class MultiHopConstants {

    /**
     * The URI of the SOAP target to indicate that the SOAP header contains the routing input for the intermediary.
     */
    public static final String NEXT_MSH_TARGET = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/nextmsh";

    /**
     * The URI to include in the <code>wsa:To</code> element when a message is sent through the I-Cloud
     */
    public static final String WSA_TO_ICLOUD = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/icloud";

    /**
     * The URI for the WS-A Action in case of a request (initiating) message in a One-Way MEP
     */
    public static final String ONE_WAY_ACTION = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay";

    /**
     * General suffix to use in the ebint:RoutingInput element
     */
    public static final String GENERAL_RESP_SUFFIX = ".response";

    /**
     * The suffix to use for the wsa:Action and MPC in case of a Receipt Signal message in a One-Way MEP
     */
    public static final String RECEIPT_SUFFIX = ".receipt";

    /**
     * The suffix to use for the wsa:Action and MPC in case of a Error Signal message in a One-Way MEP
     */
    public static final String ERROR_SUFFIX = ".error";

    /**
     * The namespace URI of the XSD that defines the RoutingInput WS-A EPR parameter
     */
    public static final String ROUTING_INPUT_NS_URI = "http://docs.oasis-open.org/ebxml-msg/ns/ebms/v3.0/multihop/200902/";

    /*
     * This class is purely a placeholder for constants related to multi-hop functionality and therefor should not be
     * instantiated.
     */
    private MultiHopConstants() {}
}

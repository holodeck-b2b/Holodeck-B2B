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
package org.holodeckb2b.common.general;

import javax.xml.namespace.QName;

/**
 * Is a placeholder for constants holding values used by Holodeck B2B and which are not already defined in other classes.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class Constants {
    
    /**
     * The fully qualified name of the xml:id attribute as specified in http://www.w3.org/TR/xml-id/
     */
    public static final QName QNAME_XMLID = new QName("http://www.w3.org/XML/1998/namespace", "id");
    
    /**
     * The URI of the ebMS namespace for the XSD that defines the ebMS message header
     */
    public static final String EBMS3_NS_URI = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
    
    /**
     * The default MPC, see section 3.4.1 of the ebMS 3 Core Specification
     */
    public static final String DEFAULT_MPC = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC";
    
    /**
     * The default role, see section 5.2.2.3 of the ebMS 3 Core Specification
     */
    public static final String DEFAULT_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultRole";
    
    /**
     * URI identifying a One-Way MEP
     */
    public static final String ONE_WAY_MEP = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay";
    /**
     * URI identifying a One-Way MEP using Push
     */
    public static final String ONE_WAY_PUSH = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push";
    /**
     * URI identifying a One-Way MEP using Pull
     */
    public static final String ONE_WAY_PULL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull";

    /**
     * URI identifying a Two-Way MEP
     */
    public static final String TWO_WAY_MEP = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay";
    /**
     * URI identifying a Two-Way MEP using Push on both legs
     */
    public static final String TWO_WAY_PUSH_PUSH = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush";
    /**
     * URI identifying a Two-Way MEP using Push on the first and Pull on the second leg
     */
    public static final String TWO_WAY_PUSH_PULL = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull";
    /**
     * URI identifying a Two-Way MEP using Pull on the first and Push on the second leg
     */
    public static final String TWO_WAY_PULL_PUSH = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush";

    
    /**
     * The name of the Holodeck B2B Core Axis2 module. 
     * NOTE: This value MUST match the <code>name</code> attribute in the <code>module.xml</code>
     */
    public static final String HOLODECKB2B_CORE_MODULE = "holodeckb2b-core";
    
    /*
     * This class is just a place holder for constants and should not be
     * instantiated! Therefore the default constructor is made private.
     */        
    private Constants() {}
}

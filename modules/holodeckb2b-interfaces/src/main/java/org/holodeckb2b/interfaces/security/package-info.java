/*
 * Copyright (C) 2019 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
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
/**
 * This package contains the interfaces related to the processing of the <i>message level security</i>. The main 
 * components, responsible for creating respectively processing the message security are defined by the {@link 
 * ISecurityHeaderCreator} and {@link ISecurityHeaderProcessor} interfaces. Access to these components is provided to 
 * the Core classes through a so-called "<i>security provider</i>" which is defined by {@link ISecurityProvider}. 
 * <o>Since Holodeck B2B's focus is on the processing of ebMS3/AS4 the current version of the interfaces are also 
 * designed with the processing of the WS-Security header contained in these messages and the security provider is only
 * required to be able to process these.<br>
 * Other protocol implementation should implement the message level security themselves. However they MUST use implement
 * the interface defined in this package to represent the result of such processing so the Core can handle them in a
 * generic, protocol independent way. Although the interfaces defining the results are also focused on the processing
 * of WS-Security most information can be mapped to them for other MLS implementation as the basic information is the
 * same.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
package org.holodeckb2b.interfaces.security;
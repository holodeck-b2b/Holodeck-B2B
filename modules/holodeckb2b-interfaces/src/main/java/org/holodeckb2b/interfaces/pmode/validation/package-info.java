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
 * This package contains the interface and classes related to the validation of P-Modes. To reduce the risk of problems
 * in the execution of message exchanges the Holodeck B2B Core will validate P-Modes before adding them to the <i>P-Mode
 * Set</i>. What however is a valid P-Mode is depends on the context, for example the used messaging protocol and the
 * environment the gateway is deployed in. Therefore <i>P-Mode validation</i> is implemented using the <i>pluggable 
 * extension<i>. Using a separate component for the validation of a P-Mode also decouples the implementation of P-Mode 
 * definition, storage and validation. E.g. an implementation of {@link IPMode} is only responsible for supplying the 
 * P-Mode parameter values as they are configured by the user.   
 * <p>
 * As there can be multiple P-Mode validators needed in a deployment the <i>Java Service Provider Interface</i> (SPI) 
 * extension mechanism is used for implementation. When a P-Mode is loaded the Core will check if a P-Mode validator is
 * registered that can validate it. This determination is based on the value of <b>PMode.MEPbinding</b> parameter. If no
 * applicable validator is found it depends on the configuration whether the P-Mode will be rejected or still be 
 * accepted.  
 *     
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  5.0.0
 */
package org.holodeckb2b.interfaces.pmode.validation;

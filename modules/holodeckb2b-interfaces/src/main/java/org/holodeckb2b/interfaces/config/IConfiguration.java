/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.config;

import java.nio.file.Path;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.Parameter;

/**
 * Defines the interface to access the Holodeck B2B <i>public</i> configuration. This public configuration contains
 * settings that may be used by extensions.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public interface IConfiguration {

	/**
	 * Gets the host name. During the message processing a host name may be needed, for example for generating a message
	 * id. Because the host name of the machine Holodeck B2B runs on may be for internal use only it is possible to set
	 * an <i>external</i> host name using the <i>ExternalHostName</i> parameter.
	 * <p>
	 * When no host name is specified in the configuration the first host name bound to a network interface (not being
	 * the loopback adapter) will be used. If there is still no host name a random id will be used.
	 *
	 * @return The host name
	 */
	String getHostName();

	/**
	 * Gets the Holodeck B2B home directory.
	 *
	 * @return The Holodeck B2B home directory.
	 */
	Path getHolodeckB2BHome();

	/**
	 * Gets the directory to use for temporarily storing files.
	 * <p>
	 * By default this the <code>temp</code> directory in the Holodeck B2B installation. The directory to use can also
	 * be specified using the <i>TempDir</i> parameter.
	 * <p>
	 * It is RECOMMENDED to create a subdirectory in this directory when regularly storing files in the temp directory.
	 *
	 * @return The absolute path to the temp directory
	 */
	Path getTempDirectory();

	/**
	 * Gets the default setting whether Errors on Errors should be reported to the sender of the faulty error. This
	 * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is
	 * often an invalid message reference. In such cases the error can not be assigned a P-Mode, so the P-Mode can not
	 * configure the behaviour.
	 *
	 * @return <code>true</code> if generated errors on errors should by default be reported to the sender,<br>
	 *         <code>false</code> otherwise
	 */
	boolean shouldReportErrorOnError();

	/**
	 * Gets the default setting whether Errors on Receipts should be reported to the sender of the faulty receipt. This
	 * setting can be overriden in the P-Mode configuration. However the problem that causes an error to be in error is
	 * often an invalid message reference. In such cases the receipt can not be assigned a P-Mode, so the P-Mode can not
	 * configure the behaviour.
	 *
	 * @return <code>true</code> if generated errors on receipts should by default be reported to the sender,<br>
	 *         <code>false</code> otherwise
	 */
	boolean shouldReportErrorOnReceipt();

	/**
	 * Gets the global setting for whether Holodeck B2B should perform a strict validation of the ebMS header meta-data
	 * as specified in the ebMS Specifications.
	 * <p>
	 * For Holodeck B2B to be able to process a message unit it does not need to conform to all the requirements as
	 * stated in the ebMS Specifications, for example the formatting of values is mostly irrelevant to Holodeck B2B.
	 * Therefore two validation modes are offered, <i>basic</i> and <i>strict</i>. This setting configures whether the
	 * strict validation mode should be used for all messages. The default is to use only basic validation.
	 * <p>
	 * Note that the P-Mode also includes a similar setting which can be used to specify the use of strict validation
	 * per P-Mode.
	 *
	 * @return <code>true</code> if a strict validation of the ebMS header meta-data should be performed for all message
	 *         units,<br>
	 *         <code>false</code> if a basic validation is enough and strict validation can be configured on P-Mode base
	 * @since 4.0.0
	 */
	boolean useStrictHeaderValidation();
	
	/**
	 * Gets the <i>custom</i> parameter with the specified name. 
	 * <p>
	 * Extensions may add their configuration parameters to the <code>holodeckb2b.xml</code> configuration file.   
	 * 
	 * @param name	the parameter name
	 * @return	the parameter if it exists or <code>null</code> when no parameter found with the specified name  
	 * @since 6.0.0
	 */
	Parameter getParameter(final String name);
}

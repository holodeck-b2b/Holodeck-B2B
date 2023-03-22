/*
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.interfaces.core;

import java.util.List;

import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;

/**
 * Represents the request parameters that are included in the query part of a URL. Parameters must be included in <code>
 * application/x-www-form-urlencoded</code> format, i.e. as "key=value" pairs separated by a '&amp;' character. When a
 * parameter is repeated in the URL, the values are grouped into a single list. Note that the query part of "http(s)"
 * URLs are case-sensitive and therefore the parameters names are so too.
 * <p>The request parameters can be retrieved from the Axis2 <code>MessageContext</code> using the {@link #get(MessageContext)}
 * method.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 */
public interface IURLRequestParameters {
	/**
	 * Name of the Axis2 MessageContext property that holds the request parameters of the current request
	 */
	public static final String	MC_PROPERTY = "hb2b:" + Constants.REQUEST_PARAMETER_MAP;

	/**
	 * Gets the set of URL parameters for the current request.
	 *
	 * @param mc	the Axis2 <code>MessageContext</code> of the current request
	 * @return		the URL parameters represented by an object of this type
	 */
	static IURLRequestParameters get(MessageContext mc) {
		try {
			return (IURLRequestParameters) mc.getProperty(MC_PROPERTY);
		} catch (ClassCastException cce) {
			return null;
		}
	}

	/**
	 * Indicates whether the URL contained a request parameter with the given name.
	 *
	 * @param name	parameter name to check for existence
	 * @return		<code>true</code> if the URL contained a parameter with the specified name,<br/>
	 * 			    <code>false</code> otherwise
	 */
	boolean contains(final String name);

	/**
	 * Indicates if the parameter with the specified name occurred more than once in the URL.
	 *
	 * @param name	parameter name to check
	 * @return		<code>Boolean.TRUE</code> if the URL contained multiple occurrences of a parameter with the
	 * 				specified name. <code>Boolean.FALSE</code> if the URL contained a single occurrence of the parameter
	 * 				with the name. <code>null</code> if the URL does not contain a parameter with the specified name.
	 */
	Boolean isMultiValue(final String name);

	/**
	 * Gets the single value of the parameter with the specified name.
	 *
	 * @param name	parameter to retrieve value of
	 * @return		String value of the parameter, <code>null</code> if no value is specified or there is no parameter
	 * 				with the specified name.
	 */
	String getValue(final String name);

	/**
	 * Gets the list of values of the parameter with the specified name.
	 *
	 * @param name	parameter to retrieve value of
	 * @return		List of Strings representing all occurrences of the parameter with the given name. Empty list if
	 * 				no values are specified or there is no parameter with the specified name.
	 */
	List<String> getValues(final String name);
}

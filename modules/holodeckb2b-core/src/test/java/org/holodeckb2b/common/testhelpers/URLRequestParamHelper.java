/*
 * Copyright (C) 2022 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.testhelpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.axis2.RequestParameters;
import org.holodeckb2b.interfaces.core.IURLRequestParameters;

/**
 * Is a helper class to add the URL request parameters to the Axis2 <code>MessageContext</code>.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 6.0.0
 */
public class URLRequestParamHelper extends RequestParameters {

	public URLRequestParamHelper() {
		super("");
	}
	
	/**
	 * Adds the given value to the list of values for the parameter with the specified name.
	 * 
	 * @param n		parameter name
	 * @param v		parameter value
	 */
	public void add(String n, String v) {
		List<String> p = params.getOrDefault(n, new ArrayList<>());
		if (p.isEmpty())
			params.put(n, p);
		p.add(v);
	}
	
	/**
	 * Adds the request parameters to the given message context.
	 * 
	 * @param msgCtx the current message context
	 */
	public void addToMsgCtx(MessageContext msgCtx) {
		msgCtx.setProperty(IURLRequestParameters.MC_PROPERTY, this);
	}
}

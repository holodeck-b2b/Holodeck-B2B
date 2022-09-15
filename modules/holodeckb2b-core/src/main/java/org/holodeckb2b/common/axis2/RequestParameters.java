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
package org.holodeckb2b.common.axis2;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.axis2.transport.http.util.URIEncoderDecoder;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.IURLRequestParameters;

/**
 * Implements the {@link IURLRequestParameters} interface to store the request parameters contained in the request URL. 
 * 
 * @author Sander Fieten (sander at chasquis-consulting.com)
 * @since 6.0.0
 */
public class RequestParameters implements IURLRequestParameters {
	protected HashMap<String, List<String>> params = new HashMap<>();
	
	/**
	 * Initialises a new instance with the parameters included in the given URL. 
	 * 
	 * @param url	the URL to retrieve the parameters from
	 */
	public RequestParameters(final URL url) {		
		retrieveFromPath(url.getPath());
	}
	
	/**
	 * Initialises a new instance with the parameters included in the given path part of an URL. 
	 * 
	 * @param urlPath	the path part of an URL to retrieve the parameters from
	 */
	public RequestParameters(final String urlPath) {
		retrieveFromPath(urlPath);
	}
	
	/**
	 * Parses the given URL path part to retrieve the request parameters. If any part of the URL query part cannot be 
	 * processed, it is ignored. This means one or more parameters may be skipped and not included in the result map.
	 * 
	 * @param urlPath	the path part of the URL
	 */	
	private void retrieveFromPath(final String urlPath) {
		if (Utils.isNullOrEmpty(urlPath))
			return;
		
		int queryPartStart = urlPath.indexOf('?');
		if (queryPartStart > 0) {
			int queryPartEnd = urlPath.indexOf(queryPartStart, '#');
			String paramPart = urlPath.substring(queryPartStart + 1, Math.max(queryPartEnd, urlPath.length()));
			String[] reqParams =  paramPart.split("&");
			for(String param : reqParams) {
				int nameEnd = param.indexOf('=');
				if (nameEnd > 0) {
					String pName = null;
					try { 
						pName = URIEncoderDecoder.decode(param.substring(0, nameEnd));
					} catch (UnsupportedEncodingException urlDecodeFailure) {
						pName = param.substring(0, nameEnd);
					}
					String pValue = null;
					try { 
						pValue = URIEncoderDecoder.decode(param.substring(nameEnd + 1));
					} catch (UnsupportedEncodingException urlDecodeFailure) {
						pValue = param.substring(nameEnd + 1);
					}
					if (!Utils.isNullOrEmpty(pName)) { 
						List<String> v = params.get(pName);
						if (v == null) {
							v = new ArrayList<>();
							params.put(pName, v);
						}
						v.add(pValue);
					}
				}
			}
		}		
	}
	
	@Override
	public boolean contains(String name) {
		return params.containsKey(name);
	}
	
	@Override
	public Boolean isMultiValue(String name) {
		List<String> v = params.get(name);
		return v == null ? null : v.size() > 1;
	}
	
	/**
	 * Gets the String value of the parameter with the given name. 
	 *  
	 * @param n		parameter name
	 * @return		the parameter value. If the parameter contained a list of values they will all be included,
	 * 				separated by a comma. If there is no parameter with the given name <code>null</code> will
	 * 				be returned		
	 */
	@Override
	public String getValue(String n) {
		List<String> lv = params.get(n);
		if (Utils.isNullOrEmpty(lv))
			return null;
		else if (lv.size() == 1)
			return lv.get(0);
		else {
			StringBuilder sv = new StringBuilder(lv.get(0));
			for (int i = 1; i < lv.size(); i++)
				sv.append(',').append(lv.get(i));
			return sv.toString();
		}
	}
	
	@Override
	public List<String> getValues(String n) {
		return params.getOrDefault(n, Collections.emptyList());		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder('[');
		for(String n : params.keySet())
			sb.append('(').append(n).append(':').append(getValue(n)).append(')');
		sb.append(']');
		return sb.toString();
	}
}
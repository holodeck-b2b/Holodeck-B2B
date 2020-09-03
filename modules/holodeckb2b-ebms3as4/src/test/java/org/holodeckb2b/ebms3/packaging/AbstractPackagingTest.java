/* Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.packaging;

import java.io.StringReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;

/**
 * Abstract base class for testing the packaging classes
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class AbstractPackagingTest {

	protected static OMElement createParent() {
		return OMAbstractFactory.getOMFactory().createOMElement("container", null);
	}
	
	protected static OMElement createXML(final String source) {		
		return OMXMLBuilderFactory.createOMBuilder(new StringReader(source)).getDocumentElement();		
	}
}

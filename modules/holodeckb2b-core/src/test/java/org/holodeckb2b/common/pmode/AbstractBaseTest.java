/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.Utils;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Is a base class for testing the classes that process XML using the Simple framework. It provides a set of utility 
 * functions to transform between XML and objects and check the XML documents. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 * @param T	The class being tested
 */
public abstract class AbstractBaseTest<T> {
	private Logger	log = LogManager.getLogger(this.getClass());
	
	@SuppressWarnings("unchecked")
	private Class<T>   type = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
																		 .getActualTypeArguments()[0];
	
	/**
     * Creates a new instance of <code>T</code> from the XML representation provided in the given string. 
     *
     * @param  type Class of object to read from XML
     * @param  xml	The XML to read the object from
     * @return 		An instance of <code>T</code> or <code>null</code> in case the given string could not be parsed
     */
	protected T createObject(final String xml) {
		
        try {      
        	return new Persister(new AnnotationStrategy()).read(type, xml);
        } catch (final InvocationTargetException ex) {
        	log.fatal("Could not parse the given XML!\n{}\n{}", Utils.getExceptionTrace(ex.getTargetException()), xml);
        	fail("Could not parse the given XML!");
        } catch (final Exception ex) {
        	log.info("Could not parse the given XML!\n{}\n{}", Utils.getExceptionTrace(ex), xml);
        }
        return null;
    }
	
	/**
     * Creates the XML representation of the give instance of <code>T</code> 
     *
     * @param  type Class of object to create XML representation of
     * @param  obj	The object to serialize
     * @return 		String containing the XML representation of the object
     */

	protected String createXML(final T obj) {
        try {        	
        	final StringWriter writer = new StringWriter();
            new Persister(new AnnotationStrategy()).write(obj, writer);
            return writer.toString();
        } catch (final InvocationTargetException ex) {
        	log.fatal("Could not create XML!\n{}", Utils.getExceptionTrace(ex.getTargetException()));
        	fail("Could not parse the given XML");            
        } catch (final Exception ex) {
        	log.info("Could not create the XML!\n{}", Utils.getExceptionTrace(ex));
        }
        return null;
    }	
	
	/**
	 * Counts the number of Elements in the provided XML with the specified local name.
	 *  
	 * @param xml			The XML to search for the elements
	 * @param localName		The local name to search for 
	 * @return				Number of elements with given local name in the XML 
	 */
	protected int countElements(final String xml, final String localName) {
		return findElements(xml, localName).size();
	}
	
	/**
	 * Finds all elements in the given XML with the given local name.
	 *  
	 * @param xml			The XML to search for the elements
	 * @param localName		The local name to search for 
	 * @return				List of Elements with the specified local name
	 */
	protected List<Element> findElements(final String xml, final String localName) {
        ArrayList<Element> result = new ArrayList<>();
		try {
        	final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);        	        	 
            NodeList elements = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)))
            												.getElementsByTagName(localName); 
            for (int i = 0; i < elements.getLength(); i++)
            	result.add((Element) elements.item(i));
        } catch (SAXException | IOException | ParserConfigurationException ex) {
			log.error("Could not parse the given XML!\n{}\n{}", Utils.getExceptionTrace(ex), xml);
		}
        
        return result;
	}
}

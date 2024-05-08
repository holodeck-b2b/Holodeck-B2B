/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.storage.metadata.jpa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

public class ReceiptTest {
	private static final DocumentBuilderFactory XML_DBF;
	static {
		XML_DBF = DocumentBuilderFactory.newInstance();
		XML_DBF.setNamespaceAware(true);
	}

	@Test
	void testContent() {
		Receipt r = new Receipt();
		List<OMElement> content = generateRcptContent();
		r.setContent(content);

		EntityManagerUtil.save(r);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        Receipt stored = em.find(Receipt.class, r.getOID());

        assertSameXML(content, stored.getContent());
	}

	@Test
	void testCopyConstructor() {
		org.holodeckb2b.common.messagemodel.Receipt source = new org.holodeckb2b.common.messagemodel.Receipt();
		source.setContent(generateRcptContent());

 		Receipt r = new Receipt(source);

		EntityManagerUtil.save(r);

		// Retrieve the object again and check value
		EntityManager em = EntityManagerUtil.getEntityManager();
		Receipt stored = em.find(Receipt.class, r.getOID());

		assertSameXML(source.getContent(), stored.getContent());
	}


	public static List<OMElement> generateRcptContent() {
		final int total = (int) Math.round(Math.random() * 50);
		List<OMElement> r = new ArrayList<>(total);
		OMFactory omf = OMAbstractFactory.getOMFactory();
		for (int i = 0; i < total; i++) {
			OMElement e = omf.createOMElement(new QName("http://test-namespace.uri", "ReceiptContent-" + i, "T"));
			e.addAttribute("someAttr", "" + i, null);
			OMElement child = omf.createOMElement(new QName("ChildElement"));
			child.addChild(omf.createOMElement(new QName("http://in-another-ns", "GrandChildElement")));
			e.addChild(child);
			r.add(e);
		}
		return r;
	}

	public static void assertSameXML(List<OMElement> xml1, List<OMElement> xml2) {
		boolean equal = xml1.size() == xml2.size();
		for (int i = 0; i < xml1.size() && equal; i++)
			equal = convert(xml1.get(i)).isEqualNode(convert(xml2.get(i)));

		assertTrue(equal);
	}

	private static Element convert(OMElement e) {
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        e.serialize(baos);
	        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	        return XML_DBF.newDocumentBuilder().parse(bais).getDocumentElement();
		} catch (Exception ex) {
			throw new RuntimeException();
		}
	}
}

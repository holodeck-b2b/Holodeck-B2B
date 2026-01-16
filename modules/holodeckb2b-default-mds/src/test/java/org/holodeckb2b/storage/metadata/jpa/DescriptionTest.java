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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Random;

import jakarta.persistence.EntityManager;

import org.holodeckb2b.storage.metadata.jpa.wrappers.WDescription;
import org.holodeckb2b.storage.metadata.testhelpers.EntityManagerUtil;
import org.junit.Test;

/**
 * Test of {@link Description} JPA class. Because it is an <i>"embeddable"</i> a wrapper class is used for testing.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class DescriptionTest {

    private static final String T_BASIC_DESCR_TEXT = "This is a very simple and quite useless description";
    private static final String T_SPECIAL_DESCR_TEXT = "Thįs dęsḉr1ptiØn has some special characters!";
    private static final String T_LANG = "en/en";

    @Test
    public void testTextAndLang() {
    	WDescription w = new WDescription();
    	w.object().setText(T_BASIC_DESCR_TEXT);
    	w.object().setLanguage(T_LANG);

    	EntityManagerUtil.save(w);

        // Retrieve the object again and check value
        EntityManager em = EntityManagerUtil.getEntityManager();
        WDescription stored = em.find(WDescription.class, w.id());

        assertNotNull(stored);
        assertEquals(T_LANG, stored.object().getLanguage());
        assertEquals(T_BASIC_DESCR_TEXT, stored.object().getText());

        em.close();
    }

    @Test
    public void testSpecialText() {
    	WDescription w = new WDescription();
    	w.object().setText(T_SPECIAL_DESCR_TEXT);

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WDescription stored = em.find(WDescription.class, w.id());

    	assertNotNull(stored);
    	assertNull(stored.object().getLanguage());
    	assertEquals(T_SPECIAL_DESCR_TEXT, stored.object().getText());

    	em.close();
    }

    @Test
    public void testTrimLongText() {
    	final String longText = createLongText();

    	WDescription w = new WDescription();
    	w.object().setText(longText);
		assertDoesNotThrow(() -> EntityManagerUtil.save(w));

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WDescription stored = em.find(WDescription.class, w.id());

    	assertNotNull(stored);
    	assertEquals(longText.substring(0, 10000), stored.object().getText());

    	em.close();
    }

    @Test
    public void testConstructor() {
    	WDescription w = new WDescription();

    	w.setObject(new Description(T_SPECIAL_DESCR_TEXT));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WDescription stored = em.find(WDescription.class, w.id());

    	assertNotNull(stored);
    	assertNull(stored.object().getLanguage());
    	assertEquals(T_SPECIAL_DESCR_TEXT, stored.object().getText());

    	em.close();
    }

    @Test
    public void testCopyConstructor() {
    	WDescription w = new WDescription();
    	final String longText = createLongText();

    	w.setObject(new Description(new org.holodeckb2b.common.messagemodel.Description(longText, T_LANG)));

    	EntityManagerUtil.save(w);

    	// Retrieve the object again and check value
    	EntityManager em = EntityManagerUtil.getEntityManager();
    	WDescription stored = em.find(WDescription.class, w.id());

    	assertNotNull(stored);
    	assertEquals(T_LANG, stored.object().getLanguage());
    	assertEquals(longText.substring(0, 10000), stored.object().getText());

    	em.close();
    }

    private String createLongText() {
    	StringBuffer b = new StringBuffer();
    	Random random = new Random();
    	for(int i = 0; i < 11000; i++)
    		b.append((char) (random.nextInt(26) + 'a'));
    	return b.toString();
    }
}

/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.common.testhelpers.TestUtils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 14:36 14.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessageIdUtilsTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private static String hostname;

    @BeforeClass
    public static void setUpClass() {
        baseDir = TestUtils.getPath("utils").toString();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
        hostname =
                HolodeckB2BCoreInterface.getConfiguration().getHostName();
    }

    @Test
    public void testCreateMessageId() throws Exception {
        String id = MessageIdUtils.createMessageId();
        assertNotNull(id);
        String[] parts = id.split("@");
        assertTrue(parts.length == 2);
        assertTrue(parts[0].length()>0);
        assertEquals(parts[1], hostname);
    }

    @Test
    public void testCreateContentId() throws Exception {
        String id = MessageIdUtils.createMessageId();
        String[] idParts = id.split("@");
        String contentId = MessageIdUtils.createContentId(id);
        assertNotNull(contentId);
        String[] contentIdParts = contentId.split("@");
        assertTrue(contentIdParts.length == 2);
        assertTrue(contentIdParts[0].contains(idParts[0]));
        assertEquals(idParts[0],
                contentIdParts[0].substring(0, contentIdParts[0].lastIndexOf("-")));
        assertTrue(contentIdParts[0].length()>0);
        assertEquals(contentIdParts[1], hostname);
    }
    
    @Test
    public void testCheckMessageId() {
    	assertTrue(MessageIdUtils.isCorrectFormat("just.a.test@holodeck-b2b.org"));
    	assertTrue(MessageIdUtils.isCorrectFormat("justatest@holodeck-b2b.org"));
    	assertTrue(MessageIdUtils.isCorrectFormat("just_a.test@holodeck-b2b.org"));
    	assertTrue(MessageIdUtils.isCorrectFormat("just.8.test@holodeck-b2b.org"));
    	assertTrue(MessageIdUtils.isCorrectFormat("`a#very$spe.cial%id!@some-where+{%&'*/}?^~|"));
    	assertFalse(MessageIdUtils.isCorrectFormat(""));
    	assertFalse(MessageIdUtils.isCorrectFormat("just.a.test"));    	
    	assertFalse(MessageIdUtils.isCorrectFormat("just[8]test@holodeck-b2b.org"));    	
    }
    
    @Test
    public void testIsAllowed() {
    	assertTrue(MessageIdUtils.isAllowed("`a#very$special%id!@some-where+{%&'*/}?^~|"));
    	assertFalse(MessageIdUtils.isAllowed(""));
    	assertFalse(MessageIdUtils.isAllowed("just a test@holodeck-b2b.org"));    	
    	assertFalse(MessageIdUtils.isAllowed("just[8]test@holodeck-b2b.org"));    	
    }
    
    
}

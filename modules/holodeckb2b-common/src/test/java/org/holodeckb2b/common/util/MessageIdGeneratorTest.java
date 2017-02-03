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

import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created at 14:36 14.01.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class MessageIdGeneratorTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private static String hostname;

    @BeforeClass
    public static void setUpClass() {
        baseDir = MessageIdGeneratorTest.class
                .getClassLoader().getResource("utils").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
        hostname =
                HolodeckB2BCoreInterface.getConfiguration().getHostName();
    }

    @Test
    public void testCreateMessageId() throws Exception {
        String id = MessageIdGenerator.createMessageId();
        assertNotNull(id);
        String[] parts = id.split("@");
        assertTrue(parts.length == 2);
        assertTrue(parts[0].length()>0);
        assertEquals(parts[1], hostname);
    }

    @Test
    public void testCreateContentId() throws Exception {
        String id = MessageIdGenerator.createMessageId();
        String[] idParts = id.split("@");
        String contentId = MessageIdGenerator.createContentId(id);
        assertNotNull(contentId);
        String[] contentIdParts = contentId.split("@");
        assertTrue(contentIdParts.length == 2);
        assertTrue(contentIdParts[0].contains(idParts[0]));
        assertEquals(idParts[0],
                contentIdParts[0].substring(0, contentIdParts[0].lastIndexOf("-")));
        assertTrue(contentIdParts[0].length()>0);
        assertEquals(contentIdParts[1], hostname);
    }
}

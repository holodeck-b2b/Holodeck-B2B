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

import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.holodeckb2b.common.exceptions.ObjectSerializationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.SortedSet;

import static org.junit.Assert.*;

/**
 * Created at 20:32 15.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class UtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Checks the presence of Mime Types that are available by means of org.apache.tika api
     */
    @Test
    public void testGetExtension() {
        assertNull(Utils.getExtension(null));
        assertNull(Utils.getExtension(""));

        final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

        SortedSet<MediaType> mTypes = allTypes.getMediaTypeRegistry().getTypes();

        for(MediaType type : mTypes) {
            MimeType mimeType = null;
            String mediaTypeName = type.toString();
            String mimeTypeName = null;
            try {
                mimeType = allTypes.getRegisteredMimeType(mediaTypeName);
                mimeTypeName = mimeType.getName();
            } catch (MimeTypeException e) {
                fail(e.getMessage());
            }

            assertEquals(mediaTypeName,mimeTypeName);

            String mimeTypeExtension = mimeType.getExtension();

            assertEquals(mimeTypeExtension, Utils.getExtension(mimeTypeName));
        }
    }

    /**
     * Test custom serialization
     * @throws Exception
     */
    @Test
    public void testSerialization() throws Exception {
        String s = "some data";
        byte[] serializedString = null;
        try {
            serializedString = Utils.serialize(s);
        } catch (Exception e) {
            fail();
        }
        assertNotNull(serializedString);
        assertEquals(s, Utils.deserialize(serializedString));
        Object o = new Object();
        try {
            Utils.serialize(o);
        } catch (Exception ose) {
            assertTrue(ose instanceof ObjectSerializationException);
        }
    }

    @Test
    public void testPreventDuplicateFileName() {
        String baseDir =
                UtilsTest.class.getClassLoader().getResource("utils").getPath();
        try {
            File dir = new File(baseDir);
            assertTrue(dir.isDirectory());
            File[] files = dir.listFiles();
            if(files.length == 0) {
                new File(baseDir + "/emptyfile.xml").createNewFile();
                new File(baseDir + "/emptyfile").createNewFile();
                files = dir.listFiles();
            }
            String newFileName1 =
                    Utils.createFileWithUniqueName(baseDir + "/emptyfile.xml").toString();
            String newFileName2 =
                    Utils.createFileWithUniqueName(baseDir + "/emptyfile").toString();
            assertNotEquals(newFileName1, newFileName2);
            for (File file : files) {
                assertNotEquals(file.getAbsolutePath(), newFileName1);
                assertNotEquals(file.getAbsolutePath(), newFileName2);
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testGetKeyByValue() {
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        assertEquals("key1", Utils.getKeyByValue(map, "value1"));
        assertEquals("key2", Utils.getKeyByValue(map, "value2"));
        assertEquals("key3", Utils.getKeyByValue(map, "value3"));
    }

    /**
     * Test possible results of {@link org.holodeckb2b.common.util.Utils#compareStrings(String, String) compareStrings}
     */
    @Test
    public void testCompareStrings() {
        assertTrue(Utils.compareStrings("a", "b") == -2);
        assertTrue(Utils.compareStrings(null, null) == -1);
        assertTrue(Utils.compareStrings(null, "") == -1);
        assertTrue(Utils.compareStrings("", null) == -1);
        assertTrue(Utils.compareStrings("", "") == -1);
        assertTrue(Utils.compareStrings("a", "a") == 0);
        assertTrue(Utils.compareStrings("a", "") == 1);
        assertTrue(Utils.compareStrings("a", null) == 1);
        assertTrue(Utils.compareStrings(null, "b") == 2);
        assertTrue(Utils.compareStrings("", "b") == 2);
    }

    @Test
    public void testGetValue() {
        assertEquals("default", Utils.getValue(null, "default"));
        assertEquals("default", Utils.getValue("", "default"));
        assertEquals("data", Utils.getValue("data", "default"));
    }
}
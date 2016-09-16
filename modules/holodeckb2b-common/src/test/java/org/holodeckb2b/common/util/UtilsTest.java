package org.holodeckb2b.common.util;

import org.holodeckb2b.common.exceptions.ObjectSerializationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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

    @Test
    public void testGetExtension() {
        assertNull(Utils.getExtension(null));
        assertNull(Utils.getExtension(""));
        assertEquals(".jpg", Utils.getExtension("image/jpeg"));
        assertEquals(".java", Utils.getExtension("text/x-java-source"));
        assertEquals(".js", Utils.getExtension("application/javascript"));
        // add more useful extensions here
    }

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
        String baseDir = UtilsTest.class.getClassLoader().getResource("utils").getPath();
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
                    Utils.preventDuplicateFileName(baseDir + "/emptyfile.xml");
            String newFileName2 =
                    Utils.preventDuplicateFileName(baseDir + "/emptyfile");
            assertNotEquals(newFileName1, newFileName2);
            for (File file : files) {
                //System.out.println(file.getAbsoluteFile());
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
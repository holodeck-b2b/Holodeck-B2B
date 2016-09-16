package org.holodeckb2b.common.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void testCompareStrings() {
        assertTrue(Utils.compareStrings("a", "b") == -2);
        assertTrue(Utils.compareStrings(null, null) == -1);
        assertTrue(Utils.compareStrings(null, "") == -1);
        assertTrue(Utils.compareStrings("", null) == -1);
        assertTrue(Utils.compareStrings("", "") == -1);
        assertTrue(Utils.compareStrings("a", "a") == 0);
        assertTrue(Utils.compareStrings("a", "") == 1); // fails
        assertTrue(Utils.compareStrings("a", null) == 1);
        assertTrue(Utils.compareStrings(null, "b") == 2);
        assertTrue(Utils.compareStrings("", "b") == 2);
    }

    @Test
    public void testName() throws Exception {

    }
}
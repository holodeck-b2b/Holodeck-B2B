package org.holodeckb2b.common.messagemodel.util;

import org.holodeckb2b.interfaces.general.IPartyId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created at 20:30 15.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CompareUtilsTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCollectionsAreEqual() {
        HashSet<IPartyId> c1 = new HashSet<IPartyId>();
        HashSet<IPartyId> c2 = new HashSet<IPartyId>();
        IPartyId p1 = new PartyIDForTest("123", "type");
        IPartyId p2 = new PartyIDForTest("124", "type1");
        c1.add(p1);
        c2.add(p1);
        assertTrue(CompareUtils.areEqual(c1, c2));
        c1.add(p2);
        c2.add(p2);
        assertTrue(CompareUtils.areEqual(c1, c2)); // fails
    }

    class PartyIDForTest implements IPartyId {
        private String id;
        private String type;

        public PartyIDForTest(String id, String type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }
    }
}
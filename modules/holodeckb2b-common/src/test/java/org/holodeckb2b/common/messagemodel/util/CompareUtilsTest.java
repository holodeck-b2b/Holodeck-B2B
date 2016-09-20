package org.holodeckb2b.common.messagemodel.util;

import org.holodeckb2b.interfaces.general.IPartyId;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.general.IService;
import org.holodeckb2b.interfaces.general.ITradingPartner;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

/**
 * Created at 20:30 15.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class CompareUtilsTest {

    @Test
    public void testTradingPartnersAreEqual() {
        IPartyId p1 = new PartyIDImpl("123", "type");
        IPartyId p2 = new PartyIDImpl("124", "type1");
        HashSet<IPartyId> c1 = new HashSet<IPartyId>();
        c1.add(p1);
        c1.add(p2);
        HashSet<IPartyId> c2 = new HashSet<IPartyId>();
        c2.add(p1);
        c2.add(p2);
        ITradingPartner partner1 = new TradingPartnerImpl(c1, "rl");
        ITradingPartner partner2 = new TradingPartnerImpl(c1, "rl");
        assertTrue(CompareUtils.areEqual(partner1, partner2));
    }

    @Test
    public void testCollectionsAreEqual() {
        HashSet<IPartyId> c1 = new HashSet<IPartyId>();
        HashSet<IPartyId> c2 = new HashSet<IPartyId>();
        IPartyId p1 = new PartyIDImpl("123", "type");
        IPartyId p2 = new PartyIDImpl("124", "type1");
        c1.add(p1);
        c2.add(p1);
        assertTrue(CompareUtils.areEqual(c1, c2));
        c1.add(p2);
        c2.add(p2);
        assertTrue(CompareUtils.areEqual(c1, c2));
    }

    @Test
    public void testPartyIdsAreEqual() {
        IPartyId p1 = new PartyIDImpl("123", "type");
        IPartyId p2 = new PartyIDImpl("123", "type");
        assertTrue(CompareUtils.areEqual(p1, p2));
    }

    @Test
    public void testPropertiesAreEqual() {
        IProperty p1 = new PropertyImpl("cAr", "tEsLA", "S");
        IProperty p2 = new PropertyImpl("cAr", "tEsLA", "S");
        assertTrue(CompareUtils.areEqual(p1, p2));
    }

    @Test
    public void testServicesAreEqual() {
        IService s1 = new ServiceImpl("123", "sap");
        IService s2 = new ServiceImpl("123", "sap");
        assertTrue(CompareUtils.areEqual(s1, s2));
    }

    class TradingPartnerImpl implements ITradingPartner {
        private Collection<IPartyId> partyIds;
        private String role;

        public TradingPartnerImpl(Collection<IPartyId> partyIds, String role) {
            this.partyIds = partyIds;
            this.role = role;
        }

        @Override
        public Collection<IPartyId> getPartyIds() {
            return partyIds;
        }

        @Override
        public String getRole() {
            return role;
        }
    }

    class PartyIDImpl implements IPartyId {
        private String id;
        private String type;

        public PartyIDImpl(String id, String type) {
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

    class PropertyImpl implements IProperty {
        private String  value;
        private String  name;
        private String  type;

        public PropertyImpl(String value, String name, String type) {
            this.value = value;
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getType() {
            return type;
        }
    }

    class ServiceImpl implements IService {
        private String  name;
        private String  type;

        public ServiceImpl(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type;
        }
    }
}
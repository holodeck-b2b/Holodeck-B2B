/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.testhelpers;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.module.HolodeckB2BCore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created at 15:22 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestUtils {

    /**
     * Returns multiplatform path
     * Is needed mainly in Windows OS to bypass the problem discribed here:
     * http://stackoverflow.com/questions/6164448/convert-url-to-normal-windows-filename-java
     * @param clazz Class instance to get class loader from
     * @param resourceName the name of the resource, which path we want to get
     * @return
     */
    public static String getPath(Class clazz, String resourceName) {
        String basePath = null;
        try {
               URL url = clazz.getClassLoader().getResource(resourceName);
               basePath = Paths.get(url.toURI()).toString();
           } catch (URISyntaxException e) {
               e.printStackTrace();
           }
        return basePath;
    }


    /**
     * Get filled mmd document for testing
     * @return
     */
    public static MessageMetaData getMMD(String resource, Object testInstance) {
        final String mmdPath =
                testInstance.getClass().getClassLoader()
                        .getResource(resource).getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        return mmd;
    }

    public static void checkPropertyElementContent(OMElement elem, String name,
                                             String text, String type) {
        assertEquals(PROPERTY_ELEMENT_NAME, elem.getQName());
        assertEquals(name, elem.getAttributeValue(new QName("name")));
        assertEquals(text, elem.getText());
        // todo see PropertyElement.createElement() method implementation
        //assertEquals(type, elem.getAttributeValue(new QName("type"))); //fail
    }

    public static void checkPropertyContent(IProperty p, String name,
                                      String value, String type) {
        assertEquals(name, p.getName());
        assertEquals(value, p.getValue());
        // todo see PropertyElement.createElement() method implementation
        //assertEquals(type, p.getType());
    }

    public static boolean eventContainsMsg(List<LoggingEvent> events, Level logLevel, String msg) {
        boolean flag = false;
        for(LoggingEvent e : events) {
            if(e.getLevel().equals(logLevel)) {
                if(e.getRenderedMessage().equals(msg)) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    public static void cleanOldMessageUnitEntities() throws Exception {
        IQueryManager queryManager = HolodeckB2BCore.getQueryManager();
        Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DAY_OF_YEAR, -0);
        String expDateString =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS.sss")
                        .format(expirationDate.getTime());
        Collection<IMessageUnitEntity> experidMsgUnits = null;
        try {
//            System.out.println("Get all test message units that changed state before "
//                    + expDateString);
            experidMsgUnits =
                    queryManager.getMessageUnitsWithLastStateChangedBefore(
                            expirationDate.getTime());
        } catch (final PersistenceException dbe) {
            System.err.println("Could not get the list of expired message units "
                    + "from database! Error details: "
                    + dbe.getMessage());
        }

        int amount = 0;
        if(experidMsgUnits != null) {
            amount = experidMsgUnits.size();
            for (final IMessageUnitEntity msgUnit : experidMsgUnits) {
                HolodeckB2BCore.getStorageManager().deleteMessageUnit(msgUnit);
            }
        }
        System.out.println("[TestUtils] Deleted " + amount
                + " test message units that changed state before "
                + expDateString);
    }

    private static final QName PROPERTY_ELEMENT_NAME =
            new QName(EbMSConstants.EBMS3_NS_URI, "Property");
}

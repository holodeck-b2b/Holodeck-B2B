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
package org.holodeckb2b.common.testhelpers;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;

import java.io.File;
import java.util.List;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 * Created at 15:22 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestUtils {
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

    public static boolean eventContainsMsg(List<LoggingEvent> events, Level logLevel, String msg) {
        boolean flag = false;
        for (LoggingEvent e : events) {
            if (e.getLevel().equals(logLevel)) {
                if (e.getRenderedMessage().trim().equals(msg)) {
                    flag = true;
                }
            }
        }
        return flag;
    }
    
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
}

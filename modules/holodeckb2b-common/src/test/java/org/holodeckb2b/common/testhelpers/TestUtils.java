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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Created at 15:42 12.05.17
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
}

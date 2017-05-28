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
package org.holodeckb2b.security.callbackhandlers;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.holodeckb2b.core.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import static org.junit.Assert.*;

/**
 * Created at 17:38 04.05.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PasswordCallbackHandlerTest {

    private static String baseDir;

    private static HolodeckB2BTestCore core;

    private PasswordCallbackHandler handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = PasswordCallbackHandlerTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new PasswordCallbackHandler();
    }

    @Test
    public void testHandle() throws Exception {
        Callback[] callbacks = new Callback[1];

        String id = "some_user";
        String pw = "password";
        String type = "some_type";
        int usage = 0;

        WSPasswordCallback callback =
                new WSPasswordCallback(id, pw, type, usage);

        callbacks[0] = callback;

        handler.addUser("some_user", "password");

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertNotNull(callback.getPassword());
    }

    @Test
    public void testHandleUnsupportedRequestCallback() throws Exception {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new Callback() {};

        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedCallbackException);
        }
    }
}
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
package org.holodeckb2b.ebms3.handlers.outflow;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.server.AxisHttpResponse;
import org.holodeckb2b.common.constants.ProductId;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.module.HolodeckB2BTestCore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created at 15:47 27.02.17
 *
 * Checked for cases coverage (04.05.2017)
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class HTTPProductIdentifierTest {

    private static final String HTTP_HDR_VALUE =  ProductId.FULL_NAME.replaceAll(" ","")
            + "/" + ProductId.MAJOR_VERSION + "." + ProductId.MINOR_VERSION;

    private static HolodeckB2BTestCore core;

    private static String baseDir;

    private HTTPProductIdentifier handler;

    @BeforeClass
    public static void setUpClass() throws Exception {
        baseDir = HTTPProductIdentifierTest.class.getClassLoader()
                .getResource("handlers").getPath();
        core = new HolodeckB2BTestCore(baseDir);
        HolodeckB2BCoreInterface.setImplementation(core);
    }

    @Before
    public void setUp() throws Exception {
        handler = new HTTPProductIdentifier();
    }

    @Test
    public void testDoProcessingForInitiator() throws Exception {
        System.out.println("[testDoProcessingForInitiator]");
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Options options = mc.getOptions();
        assertNotNull(options.getProperty(HTTPConstants.USER_AGENT));
    }

    @Test
    public void testDoProcessingForResponder() throws Exception {
        System.out.println("[testDoProcessingForResponder]");
        MessageContext mc = new MessageContext();
        mc.setFLOW(MessageContext.OUT_FLOW);
        mc.setServerSide(true);

        AxisHttpResponse response = mock(AxisHttpResponse.class);
        mc.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO, response);

        try {
            Handler.InvocationResponse invokeResp = handler.invoke(mc);
            assertNotNull(invokeResp);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        verify(response).setHeader("Server", HTTP_HDR_VALUE);
    }
}
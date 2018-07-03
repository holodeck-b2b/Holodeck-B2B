/*
 * Copyright (C) 2018 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.axis2;

import java.util.UUID;
import org.apache.axis2.AxisFault;
import static org.apache.axis2.client.ServiceClient.ANON_OUT_IN_OP;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.holodeckb2b.module.HolodeckB2BCoreImpl;

/**
 * Is a specialized version of an {@link AxisService} preconfigured for sending the ebMS3/AS4 message. It engages the
 * Holodeck B2B module and sets a specific {@link AxisOperation} that can handle both empty responses and ones that
 * contain a message.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class ebMS3SendService extends AxisService {

    public ebMS3SendService() throws AxisFault {
        super("EBMS3-SND-SVC-" + UUID.randomUUID());

        final OutOptInAxisOperation outInOperation = new OutOptInAxisOperation(ANON_OUT_IN_OP);
        addOperation(outInOperation);
        addModuleref(HolodeckB2BCoreImpl.HOLODECKB2B_CORE_MODULE);
    }
}

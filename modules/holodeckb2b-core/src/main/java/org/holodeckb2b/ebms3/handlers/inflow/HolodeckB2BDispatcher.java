/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.handlers.inflow;

import javax.xml.namespace.QName;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AbstractDispatcher;
import org.holodeckb2b.ebms3.packaging.Messaging;

/**
 * Is an Axis2 <i>Dispatcher</i> that directs message to the MSH service.
 * 
 * @author Sander Fieten
 */
public class HolodeckB2BDispatcher extends AbstractDispatcher
{
  public AxisOperation findOperation(AxisService service, MessageContext msgCtx)
                       throws AxisFault
  {
    return service.getOperation(new QName("ebms"));
  }

  public AxisService findService(MessageContext msgCtx) throws AxisFault
  {

    return msgCtx.getConfigurationContext().getAxisConfiguration().getService("msh");
  }

  public void initDispatcher() {}
}
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
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AbstractDispatcher;

/**
 * Is an Axis2 <i>Dispatcher</i> that directs message to the MSH service.
 *
 * @author Sander Fieten
 */
public class HolodeckB2BDispatcher extends AbstractDispatcher
{
  @Override
  public AxisOperation findOperation(final AxisService service, final MessageContext msgCtx)
                       throws AxisFault
  {
    return service.getOperation(new QName("Receive"));
  }

  @Override
  public AxisService findService(final MessageContext msgCtx) throws AxisFault
  {
    AxisService axisService = msgCtx.getAxisService();
    if (axisService != null)
        return axisService;
    else if (msgCtx.getTo().getAddress().endsWith("msh"))
        return msgCtx.getConfigurationContext().getAxisConfiguration().getService("as4");
    else
        return null;
  }

  @Override
  public void initDispatcher() {}
}
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
package org.holodeckb2b.as4.compression;

import java.util.ArrayList;
import java.util.Collection;

import javax.activation.DataHandler;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistency.entities.Property;
import org.holodeckb2b.ebms3.persistency.entities.UserMessage;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.interfaces.as4.pmode.IAS4PayloadProfile;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.IProperty;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.pmode.IPayloadProfile;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;

/**
 * Is the <i>OUT_FLOW</i> handler part of the AS4 Compression Feature responsible for the compression of the payload
 * data. Whether this feature should be used is indicated by the <code>PMode[1].PayloadService.CompressionType</code>
 * P-Mode parameter (defined in section 3.1 of the AS4 profile). This P-Mode parameter is represented by {@link
 * IAS4PayloadProfile#getCompressionType()}.
 * <p>When payloads should be compressed two <code>eb:Property</code> elements must be added to the
 * <code>eb:PartProperties</code> of the payload meta data in the ebMS header:<ol>
 * <li><code>@name = <i>"CompressionType"</i></code> and fixed value <i>"application/gzip"</i>;</li>
 * <li><code>@name = <i>"MimeType"</i></code> and value the MIME Type of the uncompressed data.</li></ol>
 * <p>The actual compression of the data is done by the {@link CompressionDataHandler} that will encapsulate the
 * original <code>DataHandler</code> that contains the payload data. This way the compression is only executed at the
 * moment the payload data is sent to the receiving MSH and an extra operation is prevented.
 * <p>NOTE: Although the AS4 profiles states that payloads containing already compressed data do not need to be
 * compressed Holodeck B2B will compress all payloads regardless of their content.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CompressionHandler extends AbstractUserMessageHandler {


    @Override
    protected InvocationResponse doProcessing(final MessageContext mc, final EntityProxy<UserMessage> umProxy) throws AxisFault {
        // Extract the entity object from the proxy
        final UserMessage um = umProxy.entity;

        // First check if this message contains payloads at all
        if (Utils.isNullOrEmpty(um.getPayloads()))
            return InvocationResponse.CONTINUE;

        log.debug("Check P-Mode configuration if AS4 compression must be used");
        final IUserMessageFlow flow = HolodeckB2BCoreInterface.getPModeSet().get(um.getPModeId())
                                                                    .getLegs().iterator().next().getUserMessageFlow();
        final IPayloadProfile plProfile = (flow != null ? flow.getPayloadProfile() : null);

        if ((plProfile instanceof IAS4PayloadProfile) &&
                CompressionFeature.COMPRESSED_CONTENT_TYPE.equalsIgnoreCase(((IAS4PayloadProfile) plProfile).getCompressionType())) {
            log.debug("AS4 Compression feature is used");
            // enable compression by decorating DataHandler and setting payload properties
            for (final IPayload p : um.getPayloads())
                // Only payloads contained in attachment can use compression
                if (p.getContainment() == IPayload.Containment.ATTACHMENT)
                    enableCompression(p, mc);

            log.debug("Enabled compression for all attached payloads");
        } else
            log.debug("AS4 Compression feature is not used");

        return InvocationResponse.CONTINUE;
    }

    @Override
    protected byte inFlows() {
        return OUT_FLOW | OUT_FAULT_FLOW;
    }


    private void enableCompression(final IPayload p, final MessageContext mc) {

        // Replace current datahandler of attachment with CompressionDataHandler to facilitate compression
        final String cid = p.getPayloadURI();
        final DataHandler source = mc.getAttachment(cid);
        mc.addAttachment(cid, new CompressionDataHandler(source));
        log.debug("Replaced DataHandler to enable compression");

        // Set the part properties to indicate AS4 Compression feature was used and original MIME Type
        // First ensure that there do not exists properties with this name
        final Collection<IProperty> partProperties = p.getProperties();
        final Collection<IProperty> remove = new ArrayList<>();
        for(final IProperty pp : partProperties)
            if (CompressionFeature.FEATURE_PROPERTY_NAME.equals(pp.getName())
                || CompressionFeature.MIME_TYPE_PROPERTY_NAME.equals(pp.getName()))
                remove.add(pp);
        partProperties.removeAll(remove);

        partProperties.add(new Property(CompressionFeature.FEATURE_PROPERTY_NAME,
                                        CompressionFeature.COMPRESSED_CONTENT_TYPE));
        partProperties.add(new Property(CompressionFeature.MIME_TYPE_PROPERTY_NAME, source.getContentType()));
        log.debug("Set PartProperties to indicate compression");
    }

}

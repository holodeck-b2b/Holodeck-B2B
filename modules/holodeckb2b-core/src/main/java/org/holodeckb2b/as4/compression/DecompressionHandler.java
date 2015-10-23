/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
import java.util.Iterator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.common.general.IProperty;
import org.holodeckb2b.common.messagemodel.IPayload;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.ebms3.persistent.message.UserMessage;
import org.holodeckb2b.ebms3.util.AbstractUserMessageHandler;
import org.holodeckb2b.ebms3.util.MessageContextUtils;

/**
 * Is the <i>IN_FLOW</i> handler part of the AS4 Compression Feature responsible for the decompression of the payload
 * data. When the feature is used the payload meta data, i.e. the <code>eb:PartInfo</code> element in the ebMS header
 * has a <code>eb:Property</code> descendant with a <code>name</code> attribute equal to <i>"CompressionType"</i> and
 * value <i>"application/gzip"</i> for each compressed payload. There must also be a <code>eb:Property</code> descendant 
 * with <code>name</code> <i>"MimeType"</i> which indicated the MIME Type of the uncompressed data. See section 3.1
 * from the AS4 profile for more information on this feature.
 * <p>The actual decompression of the data is done by the {@link CompressionDataHandler} that will encapsulate the 
 * original <code>DataHandler</code> that contains the payload data. This way the decompression is only executed at the
 * moment the payload data is written to an output stream and an extra operation is prevented.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class DecompressionHandler extends AbstractUserMessageHandler {

    
    @Override
    protected InvocationResponse doProcessing(MessageContext mc, UserMessage um) throws AxisFault {
        // Decompression is only needed if the message contains payloads at all
        if (Utils.isNullOrEmpty(um.getPayloads())) 
            return InvocationResponse.CONTINUE;
        
        // The compression feature can be used per payload, so check all payloads in message
        for (IPayload p : um.getPayloads()) {
            // Only payloads contained in attachment can use compression
            if (p.getContainment() == IPayload.Containment.ATTACHMENT 
               && usesCompression(p)) {
                log.debug("Payload uses compression feature, change DataHandler");
                // Replace current datahandler of attachment with CompressionDataHandler to facilitate decompression
                // First get the MIME Type the orginal data is supposed to be in
                String mimeType = null;
                for(Iterator<IProperty> pps = p.getProperties().iterator() ; pps.hasNext() && mimeType == null;) {
                    IProperty pp = pps.next();
                    mimeType = CompressionFeature.MIME_TYPE_PROPERTY_NAME.equals(pp.getName()) ? pp.getValue() : null;
                }
                if (mimeType == null || mimeType.isEmpty()) {
                    log.warn("No source MIME Type specified for compressed payload!");
                    // Generate error and stop processing this user messsage
                    DeCompressionFailure decompressFailure = new DeCompressionFailure();
                    decompressFailure.setErrorDetail("Missing required MimeType part property for compressed payload ["
                            + p.getPayloadURI() + "]!");
                    decompressFailure.setRefToMessageInError(um.getMessageId());
                    MessageContextUtils.addGeneratedError(mc, decompressFailure);
                    try {
                        um = MessageUnitDAO.setFailed(um);
                    } catch (DatabaseException dbe) {
                        // Ai, something went wrong when changing the process state
                        log.error("An error occurred when setting processing state to Failure. Details: " 
                                 + dbe.getMessage());
                    }
                } else {
                    // Replace DataHandler to enable decompression 
                    try {
                        String cid = p.getPayloadURI();
                        mc.addAttachment(cid, new CompressionDataHandler(mc.getAttachment(cid), mimeType));
                        log.debug("Replaced DataHandler to enable decompression");
                        // Remove part property specific to AS4 Compression feature
                        removeProperty(p);
                    } catch (NullPointerException npe) {
                        /* The NPE is probably caused by a missing attachment 
                            => invalid ebMS but this will be detected in the SaveUserMsgAttachment handler.
                               For now we just skip replacing the datahandler
                        */
                    }
                }
            }
        }
        
        return InvocationResponse.CONTINUE;
    }

    @Override
    protected byte inFlows() {
        return IN_FLOW | IN_FAULT_FLOW;
    }
    
    /**
     * Is a helper method to determine if the payload uses the AS4 compression feature. As described in section 3.1 of
     * the AS4 profile use of the compression feature is indicated by a <i>part property</i> named <i>"CompressionType"
     * </i> and which must have value <i>"application/gzip"</i>.
     * 
     * @param p     The payload to check for use of compression feature
     * @return      <code>true</code> if the payload uses the AS4 compression feature,<br>
     *              <code>false</code> otherwise
     */
    private boolean usesCompression(IPayload p) {
        boolean compression = false;        
        for(Iterator<IProperty> pps = p.getProperties().iterator() ; pps.hasNext() && !compression ;) {
            IProperty pp = pps.next();
            compression = CompressionFeature.FEATURE_PROPERTY_NAME.equals(pp.getName()) 
                                && CompressionFeature.COMPRESSED_CONTENT_TYPE.equals(pp.getValue());
        }
        return compression;
    }
    
    /**
     * Is a helper method that removes the part property specific to the AS4 compression feature.
     * <p>Currently only the property with name <i>"CompressionType"</i> is removed. The compression feature also adds
     * a <i>"MimeType"</i> property and optionally a <i>"CharacterSet"</i> properties but these are retained as hints
     * to the consuming business application.
     * 
     * @param p     The payload meta data object to remove the property from
     */
    private void removeProperty(IPayload p) {
        Collection<IProperty> partProperties = p.getProperties();
        Collection<IProperty> remove = new ArrayList<IProperty>();
        
        for(IProperty pp : partProperties) 
            if (CompressionFeature.FEATURE_PROPERTY_NAME.equals(pp.getName()))
                remove.add(pp);
        
        partProperties.removeAll(remove);
    }
}

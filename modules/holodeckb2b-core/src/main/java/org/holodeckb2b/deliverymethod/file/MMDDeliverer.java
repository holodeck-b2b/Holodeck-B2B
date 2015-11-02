/*
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

package org.holodeckb2b.deliverymethod.file;

import java.io.File;
import java.io.IOException;
import org.holodeckb2b.common.delivery.MessageDeliveryException;
import org.holodeckb2b.common.messagemodel.ISignalMessage;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.workers.SubmitFromFile;

/**
 * Is an {@link IMessageDeliverer} implementation that delivers <b>ONLY user message units</b> to the business 
 * application by writing the user message info to a MMD file and the payload contents to separate files in the same 
 * directory. The payload files are referred to through the <code>location</code> attribute of the <code>PartInfo</code> 
 * element in the MMD document.
 * <p>The XML format of the message meta data (MMD) document is the same as for the default file based message submitter
 * ({@link SubmitFromFile}) and defined in the <code>http://holodeck-b2b.org/schemas/2014/06/mmd</code> xml schema 
 * definition.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see FileDeliveryFactory
 * @see MessageMetaData
 */
public class MMDDeliverer extends AbstractFileDeliverer {

    public MMDDeliverer(String dir) {
        super(dir);
    }

    @Override
    protected void deliverSignalMessage(ISignalMessage sigMsgUnit) throws MessageDeliveryException {
        // Not supported, this deliverer only delivers user messages!
    }

    @Override
    protected void writeUserMessageInfoToFile(MessageMetaData mmd) throws IOException {
        String mmdFilePath = Utils.preventDuplicateFileName(directory 
                                                            + mmd.getMessageId().replaceAll("[^a-zA-Z0-9.-]", "_") 
                                                            + ".mmd.xml");
        mmd.writeToFile(new File(mmdFilePath));        
    }
    
}

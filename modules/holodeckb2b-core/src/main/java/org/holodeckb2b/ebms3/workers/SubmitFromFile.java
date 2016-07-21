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
package org.holodeckb2b.ebms3.workers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workers.DirWatcher;
import org.holodeckb2b.ebms3.mmd.xml.MessageMetaData;
import org.holodeckb2b.ebms3.mmd.xml.PartInfo;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.submit.IMessageSubmitter;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * This worker reads all MMD documents from the specified directory and submits the corresponding user message to the
 * Holodeck B2B core to trigger the send process.
 * <p>The files to process must have extension <b>mmd</b>. After processing the file, i.e. after the user message has
 * been submitted, the extension will be changed to <b>accepted</b>. When an error occurs on submit the extension will
 * be changed to <b>rejected</b> and information on the error will be written to a file with the same name but
 * with extension <b>err</b>.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SubmitFromFile extends DirWatcher {

    /**
     * Initializes the worker. Overrides parent method to ensure that the watched
     * extension is set fixed to "mmd".
     * <p>Also gets the {@see IMessageSubmitterFactory} from the Holodeck B2B core
     * to create {@see IMessageSubmitter}s for the submission of the user messages
     * to Holodeck B2B.
     *
     * @param parameters
     * @throws TaskConfigurationException
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
        // Set parameters using super class method
        super.setParameters(parameters);

        // Override externsion parameter to set it to fixed "mmd" value
        setExtension("mmd");
    }

    @Override
    protected void onChange(final File f, final Event event) {
        if (event != Event.ADDED) {
            // Only proces new mmd files, ignore other events
            log.debug(event.toString().toLowerCase() + " " + f.getName() + " ignored");
            return;
        }

        final String  cFileName = f.getAbsolutePath();
        String  bFileName = null;
        final int     i = cFileName.toLowerCase().indexOf(".mmd"); // start of extension part
        bFileName = cFileName.substring(0, i);
        final String tFileName = bFileName + ".processing";

        try {
            // Directly rename file to prevent processing by another worker
            if( !f.renameTo(new File(tFileName)))
                // Renaming failed, so file already processed by another worker or externally
                // changed
                log.info(f.getName() + " is not processed because it could be renamed");
            else {
                // The file can be processed
                log.debug("Read message meta data from " + f.getName());
                final MessageMetaData mmd = MessageMetaData.createFromFile(new File(tFileName));
                log.debug("Succesfully read message meta data from " + f.getName());
                // Convert relative paths in payload references to absolute ones to prevent file not found errors
                convertPayloadPaths(mmd, f);
                final IMessageSubmitter   submitter = HolodeckB2BCoreInterface.getMessageSubmitter();
                submitter.submitMessage(mmd, mmd.shouldDeleteFilesAfterSubmit());
                log.info("User message from " + f.getName() + " succesfully submitted to Holodeck B2B");
                // Change extension to reflect success
                new File(tFileName).renameTo(new File(bFileName + ".accepted"));
            }
        } catch (final Exception e) {
            // Something went wrong on reading the message meta data
            log.error("An error occured when reading message meta data from " + f.getName()
                        + ". Details: " + e.getMessage());
            // Change extension to reflect error and write error information
            new File(tFileName).renameTo(new File(bFileName + ".rejected"));
            writeErrorFile(bFileName + ".err", e);
        }

    }

    /**
     * Is a helper method to convert relative payload paths to absolute ones.
     *
     * @param mmd       The message meta-data document for the message to submit
     * @param mmdFile   The file handler for the MMD document, used to base path
     */
    protected void convertPayloadPaths(final MessageMetaData mmd, final File mmdFile) {
        final String basePath = mmdFile.getParent();
        if (!Utils.isNullOrEmpty(mmd.getPayloads()))
            for (final IPayload p : mmd.getPayloads()) {
                final PartInfo pi = (PartInfo) p;
                if (!(Paths.get(pi.getContentLocation()).isAbsolute()))
                    pi.setContentLocation(Paths.get(basePath, pi.getContentLocation()).normalize().toString());
            }
    }

    /**
     * Writes error information to file when a submission failed.
     *
     * @param fileName   The file name that should used be for the error file.
     * @param fault      The exception that caused the submission to fail
     */
    protected void writeErrorFile(final String fileName, final Exception fault) {
        log.debug("Writing submission error to error file: " + fileName);
        try (PrintWriter errorFile = new PrintWriter(new File(fileName))) {

            errorFile.write("The message could not be submitted to Holodeck B2B due to an error:\n\n");
            errorFile.write("Error type:    " + fault.getClass().getSimpleName() + "\n");
            errorFile.write("Error message: " + fault.getMessage() + "\n");
            errorFile.write("\n\nError details\n-------------\n");
            errorFile.write("Exception cause: " + (fault.getCause() != null
                                                                ? fault.getCause().toString() : "unknown") + "\n");
            errorFile.write("Stacktrace:\n");
            fault.printStackTrace(errorFile);

            log.debug("Error information written to file");
        } catch (final FileNotFoundException ioe) {
            log.error("Could not write error information to error file!");
        }
    }
}

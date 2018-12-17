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
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.common.mmd.xml.PartInfo;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
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
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class SubmitFromFile extends AbstractWorkerTask {
    /**
     * The path to the directory to watch for MMD files
     */
    private String watchPath;

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
        // Check the watchPath parameter is provided and points to a directory
        final String pathParameter = (String) parameters.get("watchPath");
        if (Utils.isNullOrEmpty(pathParameter)) {
            log.error("Unable to configure task: Missing required parameter \"watchPath\"");
            throw new TaskConfigurationException("Missing required parameter \"watchPath\"");
        } else if (!Paths.get(pathParameter).isAbsolute())
            watchPath = Paths.get(HolodeckB2BCoreInterface.getConfiguration().getHolodeckB2BHome(), pathParameter).toString();
        else
            watchPath = pathParameter;

        final File dir = new File(watchPath);
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            log.error("The specified directory to watch for submission [" + watchPath + "] is not accessible to HB2B");
            throw new TaskConfigurationException("Invalid path specified!");
        }
    }

    @Override
    public void doProcessing() {
        log.debug("Get list of available MMD files from watched directory: " + watchPath);
        final File   dir = new File(watchPath);
        final File[] mmdFiles = dir.listFiles(new FileFilter() {
                                        @Override
                                        public boolean accept(final File file) {
                                            return file.isFile() && file.getName().toLowerCase().endsWith(".mmd");
                                        }
                                    });
        // A null value indicates the directory could not be read => signal as error
        if (mmdFiles == null) {
            log.error("The specified directory [" + watchPath + "]could not be searched for MMD files!");
            return;
        }

        for(File f : mmdFiles) {
            // Get file name without the extension
            final String  cFileName = f.getAbsolutePath();
            final String  baseFileName = cFileName.substring(0, cFileName.toLowerCase().indexOf(".mmd"));
            final String tFileName = baseFileName + ".processing";

	        try {
	            // Directly rename file to prevent processing by another worker
	            if( !f.renameTo(new File(tFileName)))
	                // Renaming failed, so file already processed by another worker or externally
	                // changed
	                log.debug(f.getName() + " is not processed because it could not be renamed");
	            else {
	                // The file can be processed
	                log.trace("Read message meta data from " + f.getName());
	                final MessageMetaData mmd = MessageMetaData.createFromFile(new File(tFileName));
	                log.trace("Succesfully read message meta data from " + f.getName());
	                // Convert relative paths in payload references to absolute ones to prevent file not found errors
	                convertPayloadPaths(mmd, f);
	                final IMessageSubmitter   submitter = HolodeckB2BCoreInterface.getMessageSubmitter();
	                submitter.submitMessage(mmd, mmd.shouldDeleteFilesAfterSubmit());
	                log.info("User message from " + f.getName() + " succesfully submitted to Holodeck B2B");
	                // Change extension to reflect success
	                Files.move(Paths.get(tFileName), Utils.createFileWithUniqueName(baseFileName + ".accepted")
	                           , StandardCopyOption.REPLACE_EXISTING);
	            }
	        } catch (final Exception e) {
	            // Something went wrong on reading the message meta data
	            log.error("An error occured when processing message meta data from " + f.getName()
	                        + ". Details: " + e.getMessage());
	            // Change extension to reflect error and write error information
	            try {
	                final Path rejectFilePath = Utils.createFileWithUniqueName(baseFileName + ".rejected");
	                Files.move(Paths.get(tFileName), rejectFilePath, StandardCopyOption.REPLACE_EXISTING);
	                writeErrorFile(rejectFilePath, e);
	            } catch (IOException ex) {
	                // The directory where the file was originally found has gone. Nothing we can do about it, so ignore
	                log.error("An error occured while renaming the mmd file or writing the error info to file!");
	            }
	        }
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
     * @param rejectFilePath   The path to the renamed mmd file. Used to determine file name for the error file.
     * @param fault            The exception that caused the submission to fail
     */
    protected void writeErrorFile(final Path rejectFilePath, final Exception fault) {
        // Split the given path into name and extension part (if possible)
        String nameOnly = rejectFilePath.toString();
        final int startExt = nameOnly.lastIndexOf(".");
        if (startExt > 0)
            nameOnly = nameOnly.substring(0, startExt);
        final String errFileName = nameOnly + ".err";

        log.trace("Writing submission error to error file: " + errFileName);
        try (PrintWriter errorFile = new PrintWriter(new File(errFileName))) {

            errorFile.write("The message could not be submitted to Holodeck B2B due to an error:\n\n");
            errorFile.write("Error type:    " + fault.getClass().getSimpleName() + "\n");
            errorFile.write("Error message: " + fault.getMessage() + "\n");
            errorFile.write("\n\nError details\n-------------\n");
            errorFile.write("Exception cause: " + (fault.getCause() != null
                                                                ? fault.getCause().toString() : "unknown") + "\n");
            errorFile.write("Stacktrace:\n");
            fault.printStackTrace(errorFile);

            log.debug("Error information written to file");
        } catch (final IOException ioe) {
            log.error("Could not write error information to error file [" + errFileName + "]!");
        }
    }
}

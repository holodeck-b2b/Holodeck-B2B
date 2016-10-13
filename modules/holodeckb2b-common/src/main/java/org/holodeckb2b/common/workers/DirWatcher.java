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
package org.holodeckb2b.common.workers;

import java.io.File;
import java.io.FileFilter;
import java.util.Map;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Base implementation of a worker that reacts to changes in a specified directory.
 * <p>This worker watches a specified directory for changes in its file listing. Whenever
 * a change is detected {@link #onChange(java.io.File, org.holodeckb2b.common.workers.PathWatcher.Event)}
 * is called. Descendants must implement this method to do the actual processing.
 * <p>This worker has two parameters:
 * <ul><li><i>watchedPath</i>: The path to the directory to watch for changes</li>
 * <li><i>extension</i>: (optional) Only look for changes in files with the specified extension</li>
 * </ul>
 *
 * @see PathWatcher
 * @author Sander Fieten <sander@holodeck-b2b.org>
 */
public abstract class DirWatcher extends PathWatcher {

    /**
     * The optional extension filter
     */
    private String  extensionFilter;

    /**
     * Sets the extension that should be used to filter the directory listing. This
     * method is defined to allow subclasses to use a specific extension without
     * relying on the configuration file.
     *
     * @param extFilter     The extension to use for filtering the file list
     */
    protected void setExtension(final String extFilter) {
        this.extensionFilter = (extFilter != null ? extFilter.toLowerCase() : null);
    }

    /**
     * Sets the parameters.
     * <p>Two parameters can be set:
     * <ul><li><b>watchPath</b>: The path tp the directory to watch for changes</li>
     * <li><b>extension<b>: (optional) Only look for changes in files with the specified extension</li>
     * </ul>
     * <p>The first parameter is already handled by the super class, this class only reads the <i>extension</i>
     * parameter.
     *
     * @param  parameters    The parameters to configure this worker
     * @throws TaskConfigurationException   When the task can not be configured correctly
     *                                      based on the supplied parameters
     */
    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
        super.setParameters(parameters);

        setExtension((String) parameters.get("extension"));
    }

    /**
     * Gets the current list of files in the given directory filtered for the given extension.
     *
     * @return The current list of files in the directory that have the given extension
     */
    @Override
    public File[] getFileList() {
        log.debug("Get current fileslisting of [" + watchPath + "]");

        final File    dir = new File(watchPath);

        if (!(dir.exists() && dir.isDirectory())) {
            log.error("Watched directory [" + watchPath + "] does not exist or is not a directory!");
            return null;
        }

        final String ext = extensionFilter;
        final File[] fileList = dir.listFiles(new FileFilter() {
                                        @Override
                                        public boolean accept(final File file) {
                                            return file.isFile() && (ext != null ? file.getName().toLowerCase().endsWith("." + ext) : true);
                                        }
                                    });

        // Sort the retrieved file list
        Utils.sortFiles(fileList);

        return fileList;
    }
}

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

package org.holodeckb2b.common.workers;

import java.io.File;

/**
 * Base implementation of a worker that reacts to changes of a specific file.
 * <p>Whenever a change is detected {@link #onChange(java.io.File, org.holodeckb2b.common.workers.PathWatcher.Event)}
 * is called. Descendants must implement this method to do the actual processing.
 * <p>This worker has one parameter: <i>watchedPath</i>, the path to the file to watch for changes.
 * 
 * @see PathWatcher
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public abstract class FileWatcher extends PathWatcher {

    /**
     * Returns the file that should be checked for changes. 
     * 
     * @return  {@link File} handle to the file located at the given path
     */
    @Override
    protected File[] getFileList() {
        File    file = new File(watchPath);
        
        if (!file.exists() || file.isDirectory()) {
            log.warn("Watched file [" + watchPath + "] does not exist or is a directory!");
            return null;
        }
        
        return new File[] { file };
    }
    
}

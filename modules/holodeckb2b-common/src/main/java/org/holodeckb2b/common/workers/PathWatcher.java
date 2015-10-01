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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.workerpool.AbstractWorkerTask;
import org.holodeckb2b.common.workerpool.TaskConfigurationException;
import org.holodeckb2b.common.workerpool.WorkerPool;

/**
 * Is a base class for checking file system changes. Extends {@link AbstractWorkerTask} so it can be run as part of a
 * Holodeck B2B worker pool. 
 * <p>This worker takes one <code>String</code> parameter named <i>watchedPath</i> indicating the path to watch for
 * changes. Sub classes are responsible for getting the file list to check by implementing {@link #getFileList()}.
 * <p><b>NOTE:</b> When comparing files for changes the timestamp of the file system is used. This can lead to problems
 * if the file system does not handle these timestamps in milliseconds and changes occur very quickly.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @see WorkerPool
 */
public abstract class PathWatcher extends AbstractWorkerTask {

    /**
     * Meta data about one file
     */
    protected class FileListing {

        public FileListing(String path, long lastModified) {
            this.path = path;
            this.lastModified = lastModified;
        }
        
        String  path;
        long    lastModified;
    }
            
    /**
     * Enumeration of the events that can happen
     */
    public enum Event {
        ADDED, CHANGED, REMOVED
    } 
    
    /**
     * The watched directory
     */
    protected String watchPath;

    /**
     * File listing of last check
     */
    private List<FileListing> lastListing = new LinkedList<FileListing>();
        
    /**
     * Is called before processing of changes in the file list starts. 
     * <p>This method can be used by implementations to prepare for handling the changes.  
     */
    protected void doPreProcessing() {}    

    /**
     * Should handle a change in the watched path. This <b>abstract</b> method is
     * called whenever a change is detected for a file in the watched directory.
     *
     * @param   f       File handle of the file that changed
     * @param  event    The event that occurred
     */
    protected abstract void onChange(File f, Event event);
    
    /**
     * Is called after processing of changes in the file list starts. 
     * <p>This method can be used by implementations to finalize the processing of changes.  
     */
    protected void doPostProcessing() {}    
    
    /**
     * Should return the current list of files on the given path.
     * 
     * @return An array of {@link File} handles to the files currently on the path
     */
    protected abstract File[] getFileList();
    
    /**
     * Sets the parameters of the watcher.
     * <p>This base class handles setting of just one parameter: <b>watchPath</b>, the path to watch for changes. Sub 
     * class can override this method if they need additional parameters, but MUST always call this method to ensure
     * correct initialization.
     *
     * @param  parameters    The parameters to configure this worker
     * @throws TaskConfigurationException   When the task can not be configured correctly
     *                                      based on the supplied parameters
     */
    @Override
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
        if (log == null) {
            log = LogFactory.getLog(this.getClass());
        }
        if (parameters == null || parameters.isEmpty()) {
            log.error("Unable to configure task: Missing required parameter \"watchPath\"");
            throw new TaskConfigurationException("Missing required parameter \"watchPath\"");
        }
        String dir = (String) parameters.get("watchPath");
        if (dir == null || dir.isEmpty()) {
            log.error("Unable to configure task: Missing required parameter \"watchPath\"");
            throw new TaskConfigurationException("Missing required parameter \"watchPath\"");
        } else {
            watchPath = dir;
        }
        
    }

    /**
     * Checks if there were changes on the given path. The actual file list to compare is determined by the sub class 
     * in method <code>getFileList()</code>. 
     * <p>For each detected change {@link #onChange(java.io.File, org.holodeckb2b.common.workers.PathWatcher.Event)} is 
     * called. Before reporting the changes <code>doPreProcessing()</code> is called to allow a subclass to prepare for 
     * handling changed files. And after all changes are reported <code>doPostProcessing()</code> is called to finalize 
     * processing.
     * 
     * @see #getFileList() 
     * @see #doPreProcessing()
     * @see #doPostProcessing() 
     */
    @Override
    public void doProcessing() {
        doPreProcessing();
        
        log.debug("Scanning path [" + watchPath + "] for changes");
        
        // Get the current file listing
        File[] N = getFileList(); 
        if (N == null)
            N = new File[0];
        
        // Convert the last listing to simply accessible array
        FileListing[] O = lastListing.toArray(new FileListing[0]);
        // While going through the listing we build a new list of meta data on current files
        List<FileListing> C = new LinkedList<FileListing>();
        // i, j are indexes for going through both array. r,c and n count then number of removals, changes and new files
        int i = 0, j = 0, r = 0, c = 0, n = 0;
        while ( i < O.length || j < N.length) {
            if (j == N.length || (i < O.length && O[i].path.compareTo(N[j].getAbsolutePath()) < 0)) {
                try { onChange(new File(O[i].path), Event.REMOVED); }
                catch (Exception e) 
                    { log.error("Unhandled exception while processing changed file. Details: " + e.getMessage()); }
                finally { i++; r++; }
            } 
            else if (i == O.length) {
                C.add(new FileListing(N[j].getAbsolutePath(), N[j].lastModified()));
                try { onChange(N[j], Event.ADDED); }
                catch (Exception e) 
                    { log.error("Unhandled exception while processing changed file. Details: " + e.getMessage()); }
                finally { j++; n++; }
            }
            else if (O[i].path.equals(N[j].getAbsolutePath())) {
                C.add(new FileListing(N[j].getAbsolutePath(), N[j].lastModified()));
                if (O[i].lastModified < N[j].lastModified()) {
                    try { onChange(N[j], Event.CHANGED); }
                    catch (Exception e) 
                        { log.error("Unhandled exception while processing changed file. Details: " + e.getMessage()); }
                    finally { c++; }
                }
                i++; j++;
            }
            else if (O[i].path.compareTo(N[j].getAbsolutePath()) > 0) {
                C.add(new FileListing(N[j].getAbsolutePath(), N[j].lastModified()));
                try { onChange(N[j], Event.ADDED); } 
                catch (Exception e) 
                    { log.error("Unhandled exception while processing changed file. Details: " + e.getMessage()); }
                finally { j++; n++; }
            }           
        }
        
        log.debug("Scanned " + watchPath + ", " + (r+c+n) + " changes: " + r + " files removed, " 
                    + n + " files added and " + c + " files modified.");
        lastListing = C;
        
        doPostProcessing();
    }
    

    
}

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
package org.holodeckb2b.common.workerpool;

import static java.lang.Thread.sleep;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class WaitingWorker extends AbstractWorkerTask {

    public static BlockingQueue<String>     workQueue = new LinkedBlockingQueue<>();

    static final AtomicInteger instances = new AtomicInteger (0);

    int instance;

    @Override
    public void setParameters(final Map<String, ?> parameters) throws TaskConfigurationException {
        instance = instances.incrementAndGet ();
    }

    @Override
    public void doProcessing() throws InterruptedException {

        while(true) {
            final String newTask = workQueue.take();

            log.debug("[" + instance + "] The new task was: " + newTask);

            sleep(1000);
        }

    }


}

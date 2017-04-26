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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Manages a pool of <i>workers</i>. Workers are used to to execute, mostly recurring. tasks in Holodeck B2B like
 * message pulling, messaging resending, automatic reconfiguration, etc. This class abstracts the thread management and
 * scheduling of the tasks.
 * <p>A instance of <code>WorkerPool</code> is configured by an {@link IWorkerPoolConfiguration} that specifies which
 * tasks must be executed and how often.
 *
 * @todo Optimize the number of threads in the pool. Now equal to the number of worker instances to prevent that some workers will take all threads
 * @todo Add a mechanism to warn caller when an error occurs starting a task
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class WorkerPool {

    /**
     * Is a class that is used for the administration of all instances of workers running in this pool. It represent
     * one running instance of a worker.
     */
    class RunningWorkerInstance {
        /**
         * The name of the worker
         */
        String      workerName;
        /**
         * The task being executed by this worker instance
         */
        IWorkerTask task;
        /**
         * The thread the instance is running in
         */
        Future<?>   runningWorker;
    }

    /**
     * Logging facility
     */
    private final Log log;
    /**
     * The workerName of the worker pool
     */
    private final String name;
    /**
     * The current configuration of the worker pool
     */
    private IWorkerPoolConfiguration config;

    /**
     * An ScheduledExecutorService is used to manage the actual pool of worker threads
     */
    private ScheduledThreadPoolExecutor pool;

    /**
     * List of running workers.
     */
    private List<RunningWorkerInstance>    workers;

    /**
     * Create an empty worker pool with given workerName
     *
     * @param name the worker name
     */
    public WorkerPool(final String name) {
        this.name = name;

        log = LogFactory.getLog(WorkerPool.class.getName() + ":" + name);
    }

    /**
     * Create a worker pool with the given configuration
     *
     * @param config the configuraiton to use
     */
    public WorkerPool(final IWorkerPoolConfiguration config) {
        this(config.getName());
        setConfiguration(config);
    }

    /**
     * Configures this worker pool. If the worker pool was already configured the pools workers are changed accordingly.
     *
     * @param newConfig The new configuration to use for this worker pool
     * @see   IWorkerPoolConfiguration
     */
    public synchronized void setConfiguration(final IWorkerPoolConfiguration newConfig) {
        if (name != null && !name.equals(newConfig.getName())) {
            // The given configuration is not for the managed worker pool!
            log.error("Name conflict between supplied configuration and worker pool: Config is for:" + config.getName() + ", while worker pools name is:" + name);
            throw new IllegalArgumentException("Name conflict between supplied configuration and worker pool: Config is for:" + config.getName() + ", while worker pools name is:" + name);
        } else {
            log.info("Start configuration");
            if (this.config == null) {
                setup(newConfig);
            } else {
                reconfigure(newConfig);
            }

            this.config = newConfig;
            log.info("Done configuration");
        }
    }

    /**
     * Stops the worker pool and all the managed workers.
     * <p>As workers may not stop immediately a time should be specified to allow for orderly shutdown of the workers.
     * If the pool fails to stop within that time an immediate shutdown will be performed, but this may also take up
     * a minute to complete.
     *
     * @param delay The delay in seconds to wait for workers to stop
     */
    public void stop(final int delay) {
        log.info("Stopping worker pool");

        if (pool == null)
            // No pool to shutdown
            return;

        pool.shutdown();
        try {
            // Wait the given delay for workers to terminate
            if (!pool.awaitTermination(delay, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Again wait some time for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("WorkerTask pool did not terminate correctly!");
                }
            }
            log.info("Worker pool stopped");
        } catch (final InterruptedException ie) {
            log.error("Interrupt received while stopping worker pool:" + ie.getMessage());

            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cleans up the worker pool and tries to stop all workers when not stopped already
     */
    @Override
    public void finalize() throws Throwable {
        try {
            if (!pool.isTerminated()) {
                // Try to stop all workers within 10 seconds
                stop(10);
            }
        } catch (final Exception e) {
            log.error("An error occurred when stopping the pool while finalizing");
        }

        super.finalize();
    }

    /**
     * Performs the initial setup of the worker pool.
     *
     * @param newConfig The initial configuration to use
     */
    protected void setup(final IWorkerPoolConfiguration newConfig) {
        log.debug("Starting initial configuration");

        pool = new ScheduledThreadPoolExecutor(newConfig.getWorkers().size());
        workers = new ArrayList<>();

        for (final IWorkerConfiguration newWorker : newConfig.getWorkers()) {
            addWorker(newWorker);
        }

        // Check if pool size is still sufficient
        final int s = workers.size();
        if (s > pool.getCorePoolSize())
            pool.setCorePoolSize(s);
    }

    /**
     * Sets a new configuration is for this worker pool. Checks if new workers should be added to the pool or if current
     * workers must be reconfigured or removed.
     *
     * @param newConfig The new configuration
     * @see IWorkerPoolConfiguration
     */
    protected void reconfigure(final IWorkerPoolConfiguration newConfig) {
        log.debug("Reconfiguring worker pool");

        for (final IWorkerConfiguration worker : config.getWorkers()) {
            boolean remove = true;
            for (final IWorkerConfiguration newWorker : newConfig.getWorkers()) {
                if (newWorker.getName().equals(worker.getName())) {
                    remove = false;
                    // Worker is still configured, but its configuration might have changed how it must be scheduled
                    if (needsRescheduling(worker, newWorker)) {
                        // If the way worker has to be executed is changed, the current instance(s) should be removed from the
                        // queue and then added again with the new parameters
                        log.debug("Execution setup of worker " + worker.getName() + " changed, removing instances from pool");
                        removeWorker(worker.getName());
                        log.debug("Old instances of worker " + worker.getName() + "removed, reinsert with new configuration");
                        addWorker(newWorker);
                    } else if (haveWorkerTaskParametersChanged(worker, newWorker)) {
                        // Pass new configuration to all the relevant workers
                        log.debug("Task parameters of worker " + worker.getName() + "changed, reconfiguring all instances");
                        try {
                            for (final RunningWorkerInstance w : workers)
                                if (w.workerName.equals(worker.getName()))
                                    w.task.setParameters(newWorker.getTaskParameters());
                        } catch (final TaskConfigurationException ce) {
                            log.error("Reconfiguration of worker " + worker.getName() + " failed. Details: " + ce.getMessage());

                            // New configuration seems incorrect, because it is unclear whether the task may keep
                            // running using its current configuration, we stop it
                            removeWorker(worker.getName());
                        }
                    } else {
                        log.debug("No changes required for worker: " + worker.getName());
                    }
                }
            }
            if (remove) {
                // Worker is not in new configuration anymore, so stop it by removing it from
                // the queue (this means that when it running right now it will finish but not be run again
                // We have to check all running workers because there can be multiple instances running
                log.debug("Worker " + worker.getName() + " removed from configuration, removing it from pool");
                removeWorker(worker.getName());
            }
        }

        // Check if pool size is still sufficient
        final int s = workers.size();
        if (s > pool.getCorePoolSize())
            pool.setCorePoolSize(s);
    }

    /**
     * Adds a new worker to the pool based on the provided configuration
     *
     * @param workerCfg     The worker configuration data
     * @see IWorkerConfiguration
     */
    protected void addWorker(final IWorkerConfiguration workerCfg) {
        if (!workerCfg.activate())
            return;

        log.debug("Adding new worker to the pool");

        try {
            final Class<?> taskClass = Class.forName(workerCfg.getWorkerTask());

            final int numWorker = (workerCfg.getConcurrentExecutions() <= 0 ? 1 : workerCfg.getConcurrentExecutions());
            final int delay = (workerCfg.getDelay() <= 0 ? 0 : workerCfg.getDelay());
            for(int i = 0; i < numWorker; i++) {
                final RunningWorkerInstance   rWorker = new RunningWorkerInstance();
                rWorker.workerName = workerCfg.getName();
                rWorker.task = (IWorkerTask) taskClass.newInstance();
                rWorker.task.setName(workerCfg.getName() + ":" + i);
                rWorker.task.setParameters(workerCfg.getTaskParameters());

                final Interval    interval = workerCfg.getInterval();
                if (interval != null)
                    // Because the initial delay must be in same timeunit as interval it needs to be converted
                    rWorker.runningWorker = pool.scheduleWithFixedDelay(rWorker.task,
                                                               interval.getUnit().convert(delay, TimeUnit.MILLISECONDS),
                                                               interval.getLength(), interval.getUnit());
                else if (delay > 0)
                    rWorker.runningWorker = pool.schedule(rWorker.task, delay, TimeUnit.MILLISECONDS);
                else
                    rWorker.runningWorker = pool.submit(rWorker.task);

                workers.add(rWorker);
                log.debug("Added new worker instance [" + workerCfg.getName() + "] to the pool");
            }
        } catch (final ClassNotFoundException cnfe) {
            log.error("Unable to add worker " + workerCfg.getName() + " because task class [" + workerCfg.getWorkerTask() + "] could not be found");
        } catch (final InstantiationException ie) {
            log.error("Unable to add worker " + workerCfg.getName() + " because task class [" + workerCfg.getWorkerTask() + "] could not be created");
        } catch (final IllegalAccessException iae) {
            log.error("Unable to add worker " + workerCfg.getName() + " because task class [" + workerCfg.getWorkerTask() + "] could not be created");
        } catch (final ClassCastException cce) {
            log.error("Unable to add worker " + workerCfg.getName() + " because task class [" + workerCfg.getWorkerTask() + "] is not a WorkerTask object");
        } catch (final TaskConfigurationException tce) {
            log.error("Unable to add worker " + workerCfg.getName() + " because task could not be configured correctly [" + tce.getMessage() + "]");
        }
    }

    /**
     * Helper method to remove a worker from the pool. Stops all running instances of the worker and removes it from
     * the list of running workers.
     *
     * @param workerName    The name of the worker to remove from the pool
     */
    protected void removeWorker(final String workerName) {
        final List<RunningWorkerInstance> stoppedWorkers = new ArrayList<>();

        log.debug("Stopping all running instances of the worker " + workerName);
        for (final RunningWorkerInstance w : workers) {
            if (w.workerName.equals(workerName)) {
                w.runningWorker.cancel(true);
                stoppedWorkers.add(w);
            }
        }
        log.debug("Removing worker from list of running worker instances");
        workers.removeAll(stoppedWorkers);
        log.debug("All instances of worker " + workerName + " removed from the pool");
    }

    /**
     * Helper method to check whether the <i>running</i> configuration of a worker has changed, i.e. whether the new
     * configuration requires rescheduling of the worker.
     * <p>Rescheduling is needed is any of the following worker parameter changes:<ul>
     * <li>the task to execute</li>
     * <li>activation</li>
     * <li>number of concurrent executions</li>
     * <li>the interval between runs</li>
     * </ul>
     *
     * @param oldCfg    The old worker configuration
     * @param newCfg    The new worker configuration
     * @return          <code>true</code> when there was a change in configuration that requires rescheduling,
     *                  <code>false</code> if no rescheduling is neeed
     */
    protected boolean needsRescheduling(final IWorkerConfiguration oldCfg, final IWorkerConfiguration newCfg) {
        boolean equal;
        equal = (oldCfg.getWorkerTask() != null && oldCfg.getWorkerTask().equals(newCfg.getWorkerTask()))
                || (oldCfg.getWorkerTask() == null && newCfg.getWorkerTask() == null);
        equal = equal && (oldCfg.activate() == newCfg.activate());
        equal = equal && (oldCfg.getConcurrentExecutions() == newCfg.getConcurrentExecutions());
        equal = equal && (oldCfg.getInterval() == newCfg.getInterval());
        return !equal;
    }

    /**
     * Helper method to check whether the task parameters of a worker have changed.
     *
     * @param oldCfg    The old worker configuration
     * @param newCfg    The new worker configuration
     * @return          <code>true</code> when there was a change in parameters,
     *                  <code>false</code> if configuration is not changed
     */
    protected boolean haveWorkerTaskParametersChanged(final IWorkerConfiguration oldCfg, final IWorkerConfiguration newCfg) {
        boolean equal = true;
        for (final Entry<String, ?> op : oldCfg.getTaskParameters().entrySet()) {
            for (final Entry<String, ?> np : newCfg.getTaskParameters().entrySet()) {
                if (op.getKey().equals(np.getKey())) {
                    equal = equal && (op.getValue().equals(np.getValue()));
                }
            }
        }
        return !equal;
    }
}

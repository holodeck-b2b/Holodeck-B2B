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
package org.holodeckb2b.core.workerpool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.Pair;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.Interval;
import org.holodeckb2b.interfaces.workerpool.IWorkerConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerPool;
import org.holodeckb2b.interfaces.workerpool.IWorkerPoolConfiguration;
import org.holodeckb2b.interfaces.workerpool.IWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;
import org.holodeckb2b.interfaces.workerpool.WorkerPoolException;

/**
 * Manages a pool of <i>workers</i>. Workers are used to to execute, mostly recurring. tasks in Holodeck B2B like
 * message pulling, messaging resending, automatic reconfiguration, etc. This class abstracts the thread management and
 * scheduling of the tasks.
 * <p>A instance of <code>WorkerPool</code> is configured by an {@link IWorkerPoolConfiguration} that specifies which
 * tasks must be executed and how often.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class WorkerPool implements IWorkerPool {

    /**
     * Class that is used for the administration of all workers running in this pool. 
     */
    class RunningWorker {
        /**
         * The current configuration of the worker
         */
    	WorkerConfiguration config;
    	    	
    	/**
         * The actual running instances of this worker consisting of the actual task
         * object and the scheduled Future in the thread pool.
         */
        List<Pair<Future<?>, IWorkerTask>> instances = new ArrayList<>();
    }

    /**
     * Logging facility
     */
    private final Logger log;
    
    /**
     * The name of the worker pool
     */
    private final String name;

    /**
     * The configuration of this pool
     */
    private final IWorkerPoolConfiguration configuration;
    
    /**
     * Timestamp of the last refresh action
     */
    private Instant  lastRefresh;

    /**
     * The current refresh interval
     */
    private int refreshInterval;
    /**
     * The scheduled task for refreshing the configuration
     */
    private ScheduledFuture<?>	refreshTask;
    
    /**
     * An ScheduledExecutorService is used to manage the actual pool of worker threads
     */
    private ScheduledThreadPoolExecutor pool;

    /**
     * List of running workers.
     */
    private List<RunningWorker>    workers;

    /**
     * Creates a new worker pool using the configuration provided by the specified configurator. The worker pool must
     * be started by calling {@link #start()}.
     *
     * @param poolName		   name to use for identification of this worker pool
     * @param configurator 	   the {@link IWorkerPoolConfiguration} that configures this pool
     * @throws WorkerPoolException  when 
     */
    public WorkerPool(final String poolName, final IWorkerPoolConfiguration configuration) throws WorkerPoolException {
    	if (Utils.isNullOrEmpty(poolName))
    		throw new WorkerPoolException("WorkerPool must be assigned a name");
    	if (configuration == null)
    		throw new WorkerPoolException("A configuration must be specified");
    	    	
    	this.name = poolName;
    	this.configuration = configuration;
    	this.workers = new ArrayList<>();
    	log = LogManager.getLogger(WorkerPool.class.getName() + "." + name);
    }
    
    /**
     * Starts the worker pool.
     * 
     * @throws WorkerPoolException	when an error occurs in loading the configuration from the configurator or the 
     * 								initialisation of one of the workers 
     */
    public void start() throws WorkerPoolException {
    	if (pool != null)
    		return;
    		
    	log.trace("Starting up worker pool");    	
    	List<IWorkerConfiguration> workerCfgs = configuration.getWorkers();
    	refreshInterval = configuration.getConfigurationRefreshInterval();
	    if (Utils.isNullOrEmpty(workerCfgs) && refreshInterval <= 0) { 
	    	log.warn("Pool NOT STARTED because configuration contains no workers and refreshing is disabled");
	    	pool = null;
	    	return;
	    }
	    pool = new ScheduledThreadPoolExecutor(1, new PoolThreadFactory(name));
	    log.debug("Initial configuration of workers");
	    reconfigure(workerCfgs);
	    lastRefresh = Instant.now();
    	if (refreshInterval > 0) {
    		log.trace("Adding task to pool to refresh configuration");
    		refreshTask = pool.scheduleAtFixedRate(() -> refresh(), refreshInterval, refreshInterval, TimeUnit.SECONDS);
    	}
    	log.info("Pool STARTED");
    }
            
    @Override
    public boolean isRunning() {
    	return pool != null && !pool.isTerminated() && !pool.isTerminating();
    }
    
    @Override
    public void shutdown(int shutdownTime) {
    	new Thread(() -> stop(shutdownTime)).start();
    }
    
    @Override
    public String getName() {
    	return name;
    }
    
    @Override
    public int getConfigurationRefreshInterval() {
    	return configuration.getConfigurationRefreshInterval();
    }
    
    @Override
    public List<IWorkerConfiguration> getCurrentWorkers() throws WorkerPoolException {
    	if (!isRunning())
    		throw new WorkerPoolException("Pool is stopped");
    	
    	return workers.stream().map(w -> w.config).collect(Collectors.toList());
    }
       
    /**
     * If needed, reloads and applies the new configuration.
     */
    private void refresh() {
    	try {
	    	if (!configuration.hasConfigChanged(lastRefresh)) {
	    		log.trace("Configuration has not changed, nothing to do");
	    		return;
	    	}
	    	log.trace("Get new configuration from configurator");
	    	configuration.reload();
    		setConfigurationRefreshInterval(configuration.getConfigurationRefreshInterval());
    		List<IWorkerConfiguration> newConfig = configuration.getWorkers();
    		reconfigure(newConfig != null ? newConfig : new ArrayList<>());
    		
    		if (workers.isEmpty())
    			log.warn("Pool is EMPTY");
    		else
    			log.info("Configuration refreshed");
    		
    		lastRefresh = Instant.now();
    	} catch (Throwable t) {
    		log.error("An error occurred when refreshing the configuration! Error trace:\n\t{}", 
    					Utils.getExceptionTrace(t, true));
    	}    	
    }
    
    /**
     * If needed the refresh task is rescheduled for a new refresh interval
     * 
     * @param interval	the new interval value as provided by the configurator
     */
    private synchronized void setConfigurationRefreshInterval(final int interval) {
    	if (interval == refreshInterval)
    		return; // nothing changed, so no need to do anything
    	
    	log.trace("Rescheduling the automatic configuration refresh");
    	refreshInterval = interval;
		log.trace("Cancelling current refresh task");
		refreshTask.cancel(false);
    	
    	if (refreshInterval > 0) {
    		log.trace("Adding new refresh task");
    		refreshTask = pool.scheduleAtFixedRate(() -> refresh(), refreshInterval, refreshInterval, TimeUnit.SECONDS);
    		log.debug("Automatic configuration refresh set to {} seconds.", refreshInterval);
    	} else 
    		log.debug("Automatic configuration refresh disabled.");    	
    }
        
    /**
     * Cleans up the worker pool and tries to stop all workers when not stopped already
     */
    @Override
    public void finalize() throws Throwable {
        try {
    	    // Try to stop all workers within 20 seconds
            stop(20);
        } catch (final Exception e) {
            log.error("An error occurred when stopping the pool while finalizing");
        }

        super.finalize();
    }

    /**
     * Reconfigures this worker pool 
     * 
     * @param  newWorkerCfgs	the new configuration to use for the pool, as a list of worker configurations 
     */
    private synchronized void reconfigure(List<IWorkerConfiguration> newWorkerCfgs) throws WorkerPoolException {       
        log.trace("Starting configuration of the pool");
        List<IWorkerConfiguration> newWorkers = newWorkerCfgs.stream()
        											  .filter(w -> workers.stream()
        													  .noneMatch(rw -> rw.config.getName().equals(w.getName())))
        											  .collect(Collectors.toList());
        List<RunningWorker> delWorkers = workers.stream()
        										.filter(rw -> newWorkerCfgs.stream()
        													   .noneMatch(w -> rw.config.getName().equals(w.getName())))
  											  	.collect(Collectors.toList());        
        List<RunningWorker> chgWorkers = workers.stream()
        										.filter(rw -> newWorkerCfgs.stream()
        														.anyMatch(w -> rw.config.getName().equals(w.getName())
        																   && (needsRescheduling(rw.config, w)
        																	  || haveParametersChanged(rw.config, w))
        																 ))
        										.collect(Collectors.toList());
        
        List<IWorkerConfiguration> failedUpdates = new ArrayList<>();
        
        if (!delWorkers.isEmpty()) {
        	log.debug("Removing {} workers from the pool", delWorkers.size());
        	for(RunningWorker w : delWorkers) {
        		try {
        			w.instances.forEach(i -> i.value1().cancel(false));
        			workers.remove(w);
        			log.trace("{} worker removed from pool", w.config.getName());
        		} catch (Throwable t) {
        			log.error("Exception thrown when stopping current worker instance: {} - {}", 
        						t.getClass().getSimpleName(), t.getMessage());
        			failedUpdates.add(w.config);
        		}
        	}        	
        }
        
        if (!newWorkers.isEmpty()) {
        	log.debug("Adding {} new workers to the pool", newWorkers.size());
        	for(IWorkerConfiguration w : newWorkers) {
        		try {
        			addWorker(w);        			
        		} catch (Throwable t) {
        			log.error("Exception thrown when adding new worker {} to the pool: {} - {}", w.getName(), 
        						t.getClass().getSimpleName(), t.getMessage());
        			failedUpdates.add(w);
        		}        		
        	}
        }
        
        if (!chgWorkers.isEmpty()) {
        	log.debug("Changing configuration of {} workers in the pool", chgWorkers.size());
        	for(RunningWorker w : chgWorkers) {
        		final String wName = w.config.getName();
        		IWorkerConfiguration newCfg = newWorkerCfgs.stream()
        												   .filter(nwc -> w.config.getName().equals(nwc.getName()))
        												   .findFirst().get();
        		if (needsRescheduling(w.config, newCfg)) {
        			// Rescheduling => delete old instances of worker, add new ones
        			log.debug("Worker {} needs to rescheduled", wName);
        			try {
	        			log.trace("Removing old instances of {} worker", wName);
	        			w.instances.forEach(i -> i.value1().cancel(false));
	        			workers.remove(w);
	        			log.trace("Adding instances of {} worker with new configuration", wName);
	        			addWorker(newCfg);	        			
            		} catch (Throwable t) {
            			log.error("Exception thrown when rescheduling worker: {} - {}", 
            						t.getClass().getSimpleName(), t.getMessage());
            			failedUpdates.add(newCfg);
            		}        			        			
        		} else { // changed parameters only
        			log.debug("Worker {} needs to reconfigured", wName);
        			try {
	        			for(Pair<Future<?>, IWorkerTask> i : w.instances)
        					i.value2().setParameters(newCfg.getTaskParameters());
	        			log.debug("All instances of {} worker reconfigured", wName);
	        			w.config.setParameters(newCfg.getTaskParameters());
        			} catch (Throwable reconfigFailure) {
        				log.error("Exception thrown when reconfiguring worker: {} - {}", 
        						  reconfigFailure.getClass().getSimpleName(), reconfigFailure.getMessage());        				
        				failedUpdates.add(newCfg);
        				// Try rolling back the change
	        			try {
	        				log.debug("Reverting back to old configuration for worker: {}", wName);	        			
	        				for(Pair<Future<?>, IWorkerTask> i : w.instances)	        			
	        					i.value2().setParameters(w.config.getTaskParameters());
	        			} catch (Throwable rollbackError) {
	        				log.fatal("Removing {} worker from pool because configuration could not be rolled back! Error: {} - {}",
	        						  rollbackError.getClass().getSimpleName(), rollbackError.getMessage());
	        				for(Pair<Future<?>, IWorkerTask> i : w.instances)	        			
	        					i.value1().cancel(true);
	        				workers.remove(w);
	        			}        				
        			}
        		}
        	}        	
        }
        
        log.trace("Recalculate the pool size");
        int threads = 2;
        for (RunningWorker w : workers)
			threads += w.instances.size();
		log.trace("Setting pool size to : {}", threads);
        pool.setCorePoolSize(threads);
        
        if (failedUpdates.isEmpty())
        	log.debug("Worker pool reconfigured");
        else 
        	throw new WorkerPoolException(failedUpdates);
    }
    
    /**
     * Adds a single worker to the pool based on the provided configuration
     *
     * @param workerCfg     The worker configuration data
     * @throws ClassNotFoundException 		when the specified task class is not available
     * @throws IllegalAccessException 		when the specified task class cannot be accessed
     * @throws InstantiationException		when the specified task class cannot be instantiated
     * @throws ClassCastException			when the specified task class does not implement {@link IWorkerTask}
     * @throws TaskConfigurationException 	when the task cannot be correctly configured
     */
    private void addWorker(final IWorkerConfiguration workerCfg) throws ClassNotFoundException, 
    																	InstantiationException, 
    																	IllegalAccessException,
    																	ClassCastException,
    																	TaskConfigurationException {
        if (!workerCfg.activate()) 
            return;

        final String name = workerCfg.getName(); 
        final String taskClassName = workerCfg.getWorkerTask();
        if (Utils.isNullOrEmpty(taskClassName))
        	throw new TaskConfigurationException("No class name specified for worker");
    	final Class<?> taskClass = Class.forName(taskClassName);
    	final Map<String, ?> parameters = workerCfg.getTaskParameters();
        final int numWorker = (workerCfg.getConcurrentExecutions() <= 0 ? 1 : workerCfg.getConcurrentExecutions());
        final int delay = (workerCfg.getDelay() <= 0 ? 0 : workerCfg.getDelay());
        final Interval    interval = workerCfg.getInterval();
        
        final RunningWorker   rWorker = new RunningWorker();
        rWorker.config = new WorkerConfiguration(name, taskClassName, parameters, delay, numWorker, interval);
        
        log.debug("Adding {} \"{}\" instance(s) to the pool", numWorker, workerCfg.getName());
        for(int i = 0; i < numWorker; i++) {
        	IWorkerTask t = (IWorkerTask) taskClass.newInstance();
            t.setName(name + "." + i);
            t.setParameters(parameters);
            
            Future<?> f;
            if (interval != null && interval.getLength() > 0)
                // Because the initial delay must be in same timeunit as interval it needs to be converted
                f  = pool.scheduleWithFixedDelay(t, interval.getUnit().convert(delay, TimeUnit.MILLISECONDS),
                                                    interval.getLength(), interval.getUnit());
            else {
                // If the interval is set to 0, this task should run continuously, therefore wrap it
                if (interval != null && interval.getLength() == 0)
                    t = new ContinuousWorkerRunner(t);
                if (delay > 0)
                    f = pool.schedule(t, delay, TimeUnit.MILLISECONDS);
                else
                    f = pool.submit(t);
            }
            rWorker.instances.add(new Pair<Future<?>, IWorkerTask>(f, t));
            log.trace("Added new {} worker instance to the pool", name);                
        }
        workers.add(rWorker);
        log.debug("Added {} \"{}\" worker instances to the pool", numWorker, workerCfg.getName());
    }
    

    /**
     * Stops the worker pool and all the managed workers.
     * <p>As workers may not stop immediately a time should be specified to allow for orderly shutdown of the workers.
     *
     * @param delay The delay in seconds to wait for workers to stop
     */
    private void stop(final int delay) {
        if (pool == null || pool.isTerminated())
            return;

        log.debug("Stopping worker pool");
        pool.shutdown();
        try {
            // Wait the given delay for workers to terminate
            if (!pool.awaitTermination(Math.max(0, delay), TimeUnit.SECONDS)) {
                log.error("Not all tasks did terminate correctly before shutdown wait time expired!");
                pool.shutdownNow();
            }            
            log.info("Worker pool STOPPED");
        } catch (final InterruptedException ie) {
            log.warn("Interrupt received while stopping worker pool:" + ie.getMessage());
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
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
    protected boolean haveParametersChanged(final IWorkerConfiguration oldCfg, final IWorkerConfiguration newCfg) {
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
